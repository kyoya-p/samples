use burn::tensor::backend::Backend;
use burn::tensor::{Tensor, TensorData};
use burn::data::dataset::Dataset;
use burn::data::dataloader::{DataLoaderBuilder, batcher::Batcher};
use burn::train::{LearnerBuilder, metric::LossMetric};
use burn::optim::AdamConfig;
use burn::record::CompactRecorder;
use rand::Rng;

use super::model::{BoardEvaluator, BoardEvaluatorConfig, RegressionBatch};
use super::encoder::{encode_state, get_card_cost};
use super::decision::select_best_action;
use crate::{setup_initial_state, apply_action, process_automatic_steps, CardType};

#[derive(Clone, Debug)]
pub struct Experience {
    pub state_vec: Vec<f32>,
    pub reward: f32,
}

pub struct DummyDataset {
    items: Vec<Experience>,
}

impl Dataset<Experience> for DummyDataset {
    fn get(&self, index: usize) -> Option<Experience> {
        self.items.get(index).cloned()
    }
    fn len(&self) -> usize {
        self.items.len()
    }
}

#[derive(Clone, Debug)]
pub struct ExpBatcher<B: Backend> {
    device: B::Device,
}

impl<B: Backend> ExpBatcher<B> {
    pub fn new(device: B::Device) -> Self {
        Self { device }
    }
}

impl<B: Backend> Batcher<Experience, RegressionBatch<B>> for ExpBatcher<B> {
    fn batch(&self, items: Vec<Experience>) -> RegressionBatch<B> {
        let batch_size = items.len();
        let mut inputs: Vec<f32> = Vec::with_capacity(batch_size * super::encoder::STATE_DIM);
        let mut targets: Vec<f32> = Vec::with_capacity(batch_size);

        for item in items {
            inputs.extend(&item.state_vec);
            targets.push(item.reward);
        }

        let inputs_data = TensorData::from(inputs.as_slice());
        let targets_data = TensorData::from(targets.as_slice());

        let inputs_tensor = Tensor::<B, 1>::from_data(inputs_data, &self.device)
            .reshape([batch_size, super::encoder::STATE_DIM]);
        let targets_tensor = Tensor::<B, 1>::from_data(targets_data, &self.device)
            .reshape([batch_size, 1]);

        RegressionBatch {
            inputs: inputs_tensor,
            targets: targets_tensor,
        }
    }
}

pub fn generate_self_play_data<B: Backend>(model: &BoardEvaluator<B>) -> Vec<Experience> {
    let device = Default::default();
    let mut experiences = Vec::new();

    println!("Starting AI self-play to generate training data...");

    // 5ゲームの対戦を実行して高品質なデータを収集
    for game_idx in 0..5 {
        let mut state = match setup_initial_state("deck.yaml", "deck-kogyo.yaml") {
            Ok(s) => s,
            Err(_) => {
                match setup_initial_state("deck.yaml", "deck.yaml") {
                    Ok(s) => s,
                    Err(e) => {
                        println!("Failed to setup initial state: {}", e);
                        continue;
                    }
                }
            }
        };

        let mut game_states_history = Vec::new();
        let mut player_ids_history = Vec::new();
        let mut steps = 0;
        let max_steps = 150;

        while steps < max_steps {
            if state.player.life == 0 || state.opponent.life == 0 {
                break;
            }

            game_states_history.push(state.clone());
            player_ids_history.push(state.player.player_id);

            let action = match select_best_action(model, &state, &device) {
                Some(act) => act,
                None => break,
            };

            if apply_action(&mut state, &action).is_err() {
                break;
            }
            process_automatic_steps(&mut state);
            steps += 1;
        }

        let winner_id = if state.player.life > state.opponent.life {
            state.player.player_id
        } else if state.opponent.life > state.player.life {
            state.opponent.player_id
        } else {
            state.player.player_id
        };

        for (i, hist_state) in game_states_history.into_iter().enumerate() {
            let hist_player_id = player_ids_history[i];
            let base_reward = if hist_player_id == winner_id {
                1.0
            } else {
                -1.0
            };

            // 1. 相手のライフを減らす（優先度: 高、重み: 0.5）
            let opponent_life_loss = (5.0 - hist_state.opponent.life as f32) * 0.5;

            // 2. 相手のスピリットを破壊する（優先度: 中、重み: 0.2）
            let opponent_spirits = hist_state.opponent.field.iter()
                .filter(|o| o.card_type == CardType::Spirit || o.card_type == CardType::Ultimate)
                .count() as f32;
            let opponent_spirit_score = (10.0 - opponent_spirits).max(0.0) * 0.2;

            // 3. 自分のフィールドオブジェクト数（優先度: 低、重み: 0.05）
            let my_objects = hist_state.player.field.len() as f32;
            let my_object_score = my_objects * 0.05;

            // 4. ソウルコアの消費に対するペナルティ
            let soul_trash = hist_state.player.trash_cores.soul as f32;
            let soul_penalty = soul_trash * -0.05;

            // 5. 自分のフィールドにあるカードのコストに対するボーナス (コストの高い強力なカードの配置を促す)
            let mut my_field_cost = 0.0;
            for obj in &hist_state.player.field {
                my_field_cost += get_card_cost(&obj.id) as f32;
            }
            let my_cost_score = my_field_cost * 0.03;

            // 6. 自分のフィールドに「光虫の旗手ファラ」が存在する場合の追加ボーナス
            let has_fara = hist_state.player.field.iter().any(|o| o.name == "光虫の旗手ファラ");
            let fara_bonus = if has_fara { 0.5 } else { 0.0 };

            // 中間報酬を統合し、tanhでスケーリング
            let intermediate_score = opponent_life_loss + opponent_spirit_score + my_object_score + soul_penalty + my_cost_score + fara_bonus;
            let blended_reward = (base_reward * 0.4 + intermediate_score.tanh() * 0.6).clamp(-1.0, 1.0);

            let encoded = encode_state(&hist_state);
            experiences.push(Experience {
                state_vec: encoded,
                reward: blended_reward,
            });
        }

        println!("Self-play game {} completed in {} steps. Winner: Player {}", game_idx + 1, steps, winner_id);
    }

    if experiences.is_empty() {
        let mut rng = rand::thread_rng();
        for _ in 0..100 {
            let state_vec: Vec<f32> = (0..super::encoder::STATE_DIM).map(|_| rng.gen_range(-1.0..1.0)).collect();
            let reward = rng.gen_range(-1.0..1.0);
            experiences.push(Experience { state_vec, reward });
        }
    }

    experiences
}

pub fn train_model() {
    println!("Start training loop for 10 epochs...");
    
    type MyBackend = burn::backend::NdArray;
    type MyAutodiffBackend = burn::backend::Autodiff<MyBackend>;

    let device = Default::default();
    let config = BoardEvaluatorConfig::new();
    let model = config.init::<MyAutodiffBackend>(&device);

    // AIのSelf-Play対戦を用いて真のトレーニングデータを生成
    let train_data = generate_self_play_data(&model);
    let valid_data = generate_self_play_data(&model);

    let dataloader_train = DataLoaderBuilder::new(ExpBatcher::<MyAutodiffBackend>::new(device.clone()))
        .batch_size(16)
        .shuffle(42)
        .build(DummyDataset { items: train_data });

    let dataloader_valid = DataLoaderBuilder::new(ExpBatcher::<MyBackend>::new(device.clone()))
        .batch_size(16)
        .shuffle(42)
        .build(DummyDataset { items: valid_data });

    let optim = AdamConfig::new().init();

    // Learnerの構築と10エポックの実行 (チェックポイント保存を有効化)
    let learner = LearnerBuilder::new("tmp/bs_model")
        .metric_train_numeric(LossMetric::new())
        .metric_valid_numeric(LossMetric::new())
        .with_file_checkpointer(CompactRecorder::new())
        .num_epochs(10)
        .build(model, optim, 1e-4);

    let _model_trained = learner.fit(dataloader_train, dataloader_valid);

    println!("Training completed.");
}


use burn::config::Config;
use burn::module::Module;
use burn::nn::{Linear, LinearConfig, Relu};
use burn::tensor::backend::{AutodiffBackend, Backend};
use burn::tensor::Tensor;
use burn::train::{RegressionOutput, TrainOutput, TrainStep, ValidStep};

#[derive(Module, Debug)]
pub struct BoardEvaluator<B: Backend> {
    layer1: Linear<B>,
    layer2: Linear<B>,
    layer3: Linear<B>,
    activation: Relu,
}

#[derive(Config, Debug)]
pub struct BoardEvaluatorConfig {
    #[config(default = 520)]
    pub input_size: usize,
    #[config(default = 256)]
    pub hidden_size: usize,
}

impl BoardEvaluatorConfig {
    pub fn init<B: Backend>(&self, device: &B::Device) -> BoardEvaluator<B> {
        BoardEvaluator {
            layer1: LinearConfig::new(self.input_size, self.hidden_size).init(device),
            layer2: LinearConfig::new(self.hidden_size, self.hidden_size).init(device),
            layer3: LinearConfig::new(self.hidden_size, 1).init(device),
            activation: Relu::new(),
        }
    }
}

impl<B: Backend> BoardEvaluator<B> {
    pub fn forward(&self, input: Tensor<B, 2>) -> Tensor<B, 2> {
        let x = self.layer1.forward(input);
        let x = self.activation.forward(x);
        let x = self.layer2.forward(x);
        let x = self.activation.forward(x);
        let x = self.layer3.forward(x);
        burn::tensor::activation::tanh(x)
    }
}

#[derive(Clone, Debug)]
pub struct RegressionBatch<B: Backend> {
    pub inputs: Tensor<B, 2>,
    pub targets: Tensor<B, 2>,
}

impl<B: AutodiffBackend> TrainStep<RegressionBatch<B>, RegressionOutput<B>> for BoardEvaluator<B> {
    fn step(&self, batch: RegressionBatch<B>) -> TrainOutput<RegressionOutput<B>> {
        let output = self.forward(batch.inputs);
        let diff = output.clone() - batch.targets.clone();
        let loss = (diff.clone() * diff).mean().reshape([1]);
        
        let grads = loss.backward();
        TrainOutput::new(self, grads, RegressionOutput { 
            loss, 
            output, 
            targets: batch.targets 
        })
    }
}

impl<B: Backend> ValidStep<RegressionBatch<B>, RegressionOutput<B>> for BoardEvaluator<B> {
    fn step(&self, batch: RegressionBatch<B>) -> RegressionOutput<B> {
        let output = self.forward(batch.inputs);
        let diff = output.clone() - batch.targets.clone();
        let loss = (diff.clone() * diff).mean().reshape([1]);
        
        RegressionOutput { 
            loss, 
            output, 
            targets: batch.targets 
        }
    }
}

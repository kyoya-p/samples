# Neo4j リンク自動検出・構築 調査ログ

## 2026-01-16: 非構造化テキストからの関係性自動抽出手法

Neo4jにおいて、普通の文章からノードとリレーションシップを自動的に検出・構築するための主要なアプローチを調査。

### 1. LLM (大規模言語モデル) + LangChain
LLMを使用してテキストを解析し、構造化されたグラフデータに変換する。
- **手法**: `LangChain` の `LLMGraphTransformer` を使用。
- **特徴**:
    - 文脈を理解し、複雑な関係性や明示されていないリンクも推論可能。
    - OpenAI (GPT-4), Gemini, Llama 3 などの強力なモデルと連携。
- **メリット**: 抽出精度が最も高く、スキーマ定義による柔軟な制御が可能。
- **参照**: [LangChain - Graph RAG](https://python.langchain.com/docs/use_cases/graph/constructing)

#### 抽出例 (LLM + LangChain)
**入力テキスト**: 「AppleのCEOであるティム・クックは、新しいiPhoneを発表した。」
**抽出されるグラフ**:
- **Nodes**: `(:Person {id: "ティム・クック"})`, `(:Organization {id: "Apple"})`, `(:Product {id: "iPhone"})`
- **Relationships**:
  - `(:Person)-[:CEO_OF]->(:Organization)` (文脈から具体的な役職関係を抽出)
  - `(:Person)-[:ANNOUNCED]->(:Product)` (動詞からアクションを抽出)
  - `(:Product)-[:MANUFACTURED_BY]->(:Organization)` (LLMの知識による推論補完も可能)

#### 参考Webサイト (技術解説記事)
- **LangChain 公式ブログ**: [Constructing knowledge graphs from text using OpenAI functions](https://blog.langchain.dev/constructing-knowledge-graphs-from-text-using-openai-functions/)
    - Tomaz Bratanic (Neo4j) による執筆。OpenAIのFunction Calling機能を使って構造化データを抽出する具体的なコードと手法が解説されています。
- **Neo4j 公式開発者ブログ**: [Text to Knowledge Graph: The LLC approach](https://neo4j.com/developer-blog/text-to-knowledge-graph-llm/)
    - 非構造化テキストをナレッジグラフに変換するプロセスを段階的に説明しています。

---

### 2. 専用NLPライブラリ (GliNER, SpaCy)
固有表現抽出 (NER) と関係抽出に特化したモデルを使用する。
- **手法**: `GliNER` (Generalist Model for Information Extraction) などのゼロショット抽出モデルを利用。
- **特徴**:
    - 特定のエンティティタイプや関係性をルールまたはモデルで抽出。
    - LLMと比較して軽量・高速・低コスト。
- **メリット**: 大規模な処理においてコスト効率が良い。

#### 抽出例 (NLPライブラリ)
**入力テキスト**: 「AppleのCEOであるティム・クックは、新しいiPhoneを発表した。」
**抽出されるグラフ**:
- **Nodes**: `(:Person {name: "ティム・クック"})`, `(:ORG {name: "Apple"})`, `(:PRODUCT {name: "iPhone"})`
- **Relationships**:
  - `(:Person)-[:WORKS_FOR]->(:ORG)` (一般的な所属関係として抽出される傾向)
  - `(:Person)-[:RELATED_TO]->(:PRODUCT)` (構文解析による係り受け関係)
  - ※「CEO_OF」のような具体的なラベル付けには、追加のルール定義やカスタム学習が必要な場合が多い。

---

### 3. Neo4j APOC NLP
Neo4jの拡張ライブラリ (APOC) を使用して、外部のクラウドNLPサービスと連携する。
- **手法**: `apoc.nlp.aws.*` や `apoc.nlp.gcp.*` プロシージャの利用。
- **特徴**:
    - AWS ComprehendやGoogle Natural Language APIの結果を直接グラフに取り込む。
- **メリット**: データベース内で処理が完結する。
- **参照**: [Neo4j APOC Documentation - NLP](https://neo4j.com/labs/apoc/4.1/nlp/)

#### 抽出例 (APOC NLP - Google Cloud NL等)
**入力テキスト**: 「AppleのCEOであるティム・クックは、新しいiPhoneを発表した。」
**抽出されるグラフ**:
- **Nodes**: `(:Person {text: "ティム・クック"})`, `(:Organization {text: "Apple"})`, `(:ConsumerGood {text: "iPhone"})`
- **Relationships**:
  - 直接的な意味的リンク（例: CEO_OF）は自動生成されにくい。
  - **共起グラフ**: 全ノードが元の `(:Article)` ノードに接続される形が一般的。
    - `(:Person)-[:ENTITY_IN_CONTENT]->(:Article)<-[:ENTITY_IN_CONTENT]-(:Organization)`
  - グラフクエリで「同じ記事に出現した」という間接的な関係として扱うことが多い。

---

### 4. N-Gram (キーワード共起)
統計的な手法を用いて、文書内での単語の出現頻度や共起関係からリンクを生成する。
- **手法**: `N-Gram` で複合語を抽出し、共起ネットワークを構築。
- **特徴**:
    - **高速・低コスト**: 機械学習モデルを必要とせず、統計処理のみで完結する。
    - **浅い関係性**: 「一緒に使われている」ことはわかるが、「どういう関係か (CEOである、発表した)」という意味は抽出できない。
- **メリット**: 専門用語辞書がない未知のドメインや、計算リソースが極端に制限される環境で有効。
- **限界**: 文脈を理解しないため、ノイズが多くなりがち。LLMやNLPモデルの前処理（候補抽出）として使われることが多い。

#### 抽出例1: 共起ネットワーク (Co-occurrence Network)
単語同士が「近くにある」ことをリンクで表現する。意味的なつながりの強さを分析するのによく使われる。
**入力テキスト**: 「ティム・クックはiPhoneを発表した」
**抽出されるグラフ**:
- **Nodes**: `(:Word {text: "ティム・クック"})`, `(:Word {text: "iPhone"})`, `(:Word {text: "発表"})`
- **Relationships**:
  - `(:Word)-[:CO_OCCURRED {weight: 1}]-(:Word)`
  - (例) `(ティム・クック)-[:CO_OCCURRED]-(iPhone)`
  - ※ リンク自体には「CEOである」などの意味ラベルはなく、「関連度」の重みのみが保存される。

#### 抽出例2: N-Gram 連鎖 (Markov Chain)
テキスト内での単語の「並び順」をリンクとして表現する。文章生成やフレーズ検索に使われる。
**入力テキスト**: 「Apple の CEO」 (Bigram / 2-gram)
**抽出されるグラフ**:
- **Nodes**: `(:Token {text: "Apple"})`, `(:Token {text: "の"})`, `(:Token {text: "CEO"})`
- **Relationships**:
  - `(Apple)-[:NEXT]->(の)`
  - `(の)-[:NEXT]->(CEO)`
  - ※ グラフ上で `(Apple)-[:NEXT*2]->(CEO)` のパスを検索することで、「Apple...CEO」というフレーズを検出できる。

---

### 5. Neo4j LLM Knowledge Graph Builder
Neo4jが提供する公式のGUIツール/アプリケーション。
- **概要**: PDFやウェブページをアップロードするだけで、LLMが自動的にグラフを構築。
- **用途**: 迅速なプロトタイピングや、既存ドキュメントの一括インポートに最適。
- **参照**: [Neo4j LLM Knowledge Graph Builder](https://github.com/neo4j-labs/llm-graph-builder)

---

### 6. ローカルLLMの活用 (LangChain連携)
LangChainはローカルLLMとの連携を強力にサポートしており、データプライバシーやコストの観点から推奨されるアプローチの一つです。

#### 利用可能なツール
- **Ollama**: Llama 3, Mistral などのモデルをローカルで手軽に実行可能。LangChainの `ChatOllama` クラスで直接利用可能。
- **Llama.cpp**: `LlamaCpp` クラスを通じてGGUF形式のモデルをロードして利用可能。
- **LM Studio / LocalAI**: OpenAI互換APIを提供するローカルサーバーとして動作させ、LangChainの `ChatOpenAI` クラス（`base_url`指定）で接続可能。

#### グラフ構築 (`LLMGraphTransformer`) における注意点
- **モデル性能**: グラフ抽出は複雑なタスクであるため、パラメータ数の少ないモデル（7B以下など）では、JSON形式の崩れや関係性の誤検出が発生しやすい。
- **推奨モデル**: `Llama 3` (8B以上) や `Mistral` (7B Instruct v0.3) など、Function Callingや構造化出力に強いモデルが推奨される。
- **コンテキストサイズ**: 長文を一度に処理する場合、ローカルLLMのコンテキストウィンドウ制限に注意が必要。

#### 推奨ハードウェアスペック (Llama 3 8B の場合)
快適に動作させるためのPC性能の目安です。

- **VRAM (GPUメモリ)**:
  - **4GB**: 最低ライン。4bit量子化 (Q4) モデルがギリギリ動作するレベル。
  - **8GB**: 推奨ライン。8bit量子化 (Q8) や、Q4モデルを余裕を持って動かせる。
  - **12GB以上**: 快適。長いコンテキスト (会話履歴や長文) を扱える。
- **システムメモリ (RAM)**:
  - **16GB**: 最低限。
  - **32GB以上**: 推奨。OSや他のアプリ、Neo4j自体のメモリ消費も考慮すると32GBあると安心。
- **GPU**: NVIDIA GeForce RTX 3060 / 4060 以上 (CUDAコアが利用できると高速)。Macの場合は M1/M2/M3 チップ (メモリ共有なので16GB以上のモデル推奨)。

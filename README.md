# enshud — 教育用コンパイラ処理系（Pascal風）

`enshud` は Pascal 風のソースコードを **字句解析 → 構文解析 → 意味解析 → コード生成 → 仮想実行** の各段階で処理する、学習用の Java プロジェクトです。入力プログラムから CASL（簡易アセンブリ）を生成し、同梱のシミュレータで実行できます。
個人学習用のため動作保証などは出来ません。

> 典型的な実行フロー
> ```bash
> $ java -jar enshud.jar enshud.Main lexer    data/pas/normal01.pas tmp/out.ts
> $ java -jar enshud.jar enshud.Main parser   data/ts/normal01.ts
> $ java -jar enshud.jar enshud.Main checker  data/ts/normal01.ts
> $ java -jar enshud.jar enshud.Main compiler data/ts/normal01.ts   tmp/out.cas
> $ java -jar enshud.jar enshud.Main casl     tmp/out.cas
> ```


## 特徴
- 各ステージ（`lexer`/`parser`/`checker`/`compiler`/`casl`）を **個別に実行可能**  
- Java/Gradle ベースで **ビルドが簡単**  
- `data/` に **サンプル入力**、`tmp/` に **出力**（生成物）を保存

## プロジェクト構成
```
src/main/java/enshud/
  ├─ Main.java          # エントリポイント
  ├─ s1/                # 字句解析
  ├─ s2/                # 構文解析
  ├─ s3/                # 意味解析
  ├─ s4/                # コード生成
  └─ casl/              # CASL シミュレータ
data/
  ├─ pas/               # Pascal風ソースの例
  └─ ts/                # トークン列（拡張子 .ts）
tmp/                    # 生成物の出力先（.ts / .cas 等）
build.gradle, settings.gradle, gradlew, gradlew.bat など
```

## 動作要件
- Java 11 以上（OpenJDK 推奨）
- Gradle 7 以上（同梱の `./gradlew` を使うのが簡単）
- OS: Windows / macOS / Linux

## ビルドと実行

### 1) 依存関係の取得とビルド
```bash
# 初回のみ（推奨）
./gradlew clean build
```

### 2) 実行方法（いずれか）

**A. JAR を直接実行（推奨）**  
ビルド済み JAR が `build/libs/` に生成されます。

```bash
# 例: クラスパスで起動（Fat JAR でない場合）
java -cp build/libs/<生成されたjar名>.jar enshud.Main lexer data/pas/normal01.pas tmp/out.ts
```

> プロジェクトに Fat JAR 生成タスク（例: `shadowJar` や `fatJar`）がある場合は、生成物を `java -jar` で起動してください。

**B. Gradle から実行（Application プラグイン有効時）**  
`build.gradle` に `application` プラグインが設定されている場合:
```bash
./gradlew run --args "lexer data/pas/normal01.pas tmp/out.ts"
```

## 使い方

### 字句解析（lexer）
```bash
java -cp build/libs/<jar>.jar enshud.Main lexer <input.pas> <output.ts>
```

### 構文解析（parser）
```bash
java -cp build/libs/<jar>.jar enshud.Main parser <input.ts>
```

### 意味解析（checker）
```bash
java -cp build/libs/<jar>.jar enshud.Main checker <input.ts>
```

### コンパイル（compiler → CASL）
```bash
java -cp build/libs/<jar>.jar enshud.Main compiler <input.ts> <output.cas>
```

### CASL 実行（仮想マシン）
```bash
java -cp build/libs/<jar>.jar enshud.Main casl <input.cas>
```

> 既定では、入力例は `data/`、出力は `tmp/` を想定しています。必要に応じてパスを調整してください。

## テスト
```
./gradlew test
```

## 開発のヒント
- それぞれのステージは **独立してテスト** できます。例えば、Lexer のみを実装して出力 `.ts` を確認してから Parser を進める、など。  
- 文法やトークン定義を更新した場合は、**サンプルと期待値**（`data/` 内）も合わせて更新し、`README` の使用例を最新化してください。
- 生成物（`.ts`, `.cas`, ログ）は **`tmp/` 配下**に保存し、コミット対象にしないのが無難です。


## 謝辞
本プロジェクトはコンパイラ構成の学習を目的としており、関係する教材や参考資料の貢献に感謝します。

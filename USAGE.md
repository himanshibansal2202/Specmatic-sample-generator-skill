# Generate Specmatic Sample Skill

This repository is the portable `generate-specmatic-sample` skill package.

## Enable In Codex

Install the repo as a Codex skill directory:

```bash
mkdir -p ~/.codex/skills
git clone https://github.com/himanshibansal2202/Specmatic-sample-generator-skill.git ~/.codex/skills/generate-specmatic-sample
```

Codex reads the root `SKILL.md` and the bundled `guides/`, `config/`, and `test-data/` files as needed.

## Enable In Claude Code

Install the repo as a Claude Code personal skill:

```bash
mkdir -p ~/.claude/skills
git clone https://github.com/himanshibansal2202/Specmatic-sample-generator-skill.git ~/.claude/skills/generate-specmatic-sample
```

Claude Code reads the root `SKILL.md` and the bundled `guides/`, `config/`, and `test-data/` files as needed.

## Example Prompts

### Spec-driven (recommended)

```
Read the SKILL.md file and all referenced files. Then follow the generate workflow.

Inputs:
1. Contract repo: https://github.com/specmatic/specmatic-order-contracts.git
   Spec: io/specmatic/examples/store/openapi/api_order_v5.yaml
2. Application type: backend
3. Language: java
4. Framework: spring-boot
5. Specmatic integration mode: native
6. Destination path: /tmp/my-sample

Skip interactive questions and proceed through all workflow steps.
Report test counts at each level.
```

### Interactive

```
Read all files in /path/to/generate-specmatic-sample/ (SKILL.md, config/*, guides/*, test-data/*).
Then follow the SKILL.md workflow — ask me for the inputs interactively.
```

### Non-REST protocol

```
Read all files in /path/to/generate-specmatic-sample/ (SKILL.md, config/*, guides/*, test-data/*).
Then follow the SKILL.md workflow to generate a Backend gRPC sample using Kotlin + Spring Boot + docker-cli.
```

## Generated Output

Generated samples are written as folders named from the selected enum values:

```text
<destination>/
└── backend-rest-java-spring-boot/
    ├── specmatic.yaml
    ├── .github/workflows/ci.yml
    └── ...
```

The Specmatic integration mode is not included in the folder name.

## Quick Test Commands

After generation, verify with:

```bash
# Java/Spring Boot (Gradle)
cd <sample-folder> && ./gradlew test

# Java/Spring Boot (Maven)
cd <sample-folder> && ./mvnw test

# JavaScript/Express
cd <sample-folder> && npm install && npm test

# Python/Flask
cd <sample-folder> && pip install -r requirements.txt && pytest test -v -s
```

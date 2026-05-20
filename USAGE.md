# Generate Specmatic Sample Skill

This repository is the portable `generate-specmatic-sample` skill package.

## Enable In Codex

Install the repo as a Codex skill directory:

```bash
mkdir -p ~/.codex/skills
git clone <repo-url> ~/.codex/skills/generate-specmatic-sample
```

Codex reads the root `SKILL.md` and the bundled `contracts/`, `guides/`, `config/`, and `test-data/` files as needed.

For project-local use while developing this repo, Codex can use the wrapper at:

```text
.codex/skills/generate-specmatic-sample/SKILL.md
```

The wrapper delegates to the root `SKILL.md` so the canonical instructions stay in one place.

## Enable In Claude Code

Install the same repo as a Claude Code personal skill:

```bash
mkdir -p ~/.claude/skills
git clone <repo-url> ~/.claude/skills/generate-specmatic-sample
```

For project-local use while developing this repo, Claude Code can also discover the wrapper at:

```text
.claude/skills/generate-specmatic-sample/SKILL.md
```

The wrapper delegates to the root `SKILL.md` so the canonical instructions stay in one place.

## Kiro Invocation

To use this skill in Kiro, start a new session and say:

```
Read all files in /path/to/generate-specmatic-sample/ (SKILL.md, contracts/*, guides/*, test-data/*, config/*).
Then follow the SKILL.md workflow to generate a Backend REST sample using JavaScript + Express + in-memory.
Ask me for the destination path or repository link, create the sample as a self-contained folder under that location, install deps, and run tests until green.
```

For non-REST protocols, name the protocol in the same prompt:

```text
Read all files in /path/to/generate-specmatic-sample/ (SKILL.md, config/*, guides/*, test-data/*).
Then follow the SKILL.md workflow to generate a Backend gRPC sample using Kotlin + Spring Boot.
Use official Specmatic sample/contract sources when configured, ask for a destination path, install deps, and run tests until green.
```

For regression testing (generate all predefined combinations):

```text
Read all files in /path/to/generate-specmatic-sample/ (SKILL.md, config/*, guides/*, test-data/*).
Run regression — generate all combinations from config/regression-matrix.yaml, verify each locally, and push to the configured destination repo.
```

For maintaining existing samples (test and fix):

```text
Read all files in /path/to/generate-specmatic-sample/ (SKILL.md, config/*, guides/*, test-data/*).
Maintain the samples at /path/to/specmatic-order-samples — run tests on all samples, fix any failures, and push fixes.
```

Or for the interactive flow:

```
Read all files in /path/to/generate-specmatic-sample/ (SKILL.md, contracts/*, guides/*, test-data/*, config/*).
Then follow the SKILL.md workflow — ask me for the inputs interactively.
```

Generated samples are written as folders named from the selected enum values:

```text
<provided-location>/
└── backend-rest-javascript-express-in-memory/
    ├── specmatic.yaml
    ├── contracts/
    ├── .github/workflows/ci.yml
    └── ...
```

Protocol aliases are normalized by the skill:

- `rest` / `openapi`
- `kafka` / `asyncapi`
- `grpc`
- `graphql`
- `soap` / `wsdl`

Some non-REST protocols use Specmatic Enterprise in the official docs and sample
projects. Generated samples for those protocols should document the required
Docker image/artifact and any license/setup prerequisites.

The provided location is only a container for sample folders. The skill should not create shared contracts, workflows, or metadata at that root.

## Quick Test Commands

After generation, verify with:
```bash
# JavaScript/Express
npm install && npm test

# Java/Spring Boot
./mvnw test

# Python/Flask
pip install -r requirements.txt && pytest test -v -s
```

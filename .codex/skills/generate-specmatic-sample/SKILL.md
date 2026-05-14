---
name: generate-specmatic-sample
description: Generate a working Specmatic v3 sample project for a given tech stack. Use when the user wants to create a Backend, BFF, or Frontend sample that demonstrates Specmatic contract testing. Triggers on requests like "generate a specmatic sample", "create a sample project for Java Spring Boot", or "scaffold a backend REST API with contract tests".
---

# Generate Specmatic Sample

This is the Codex project-skill entrypoint for the portable skill stored at the repository root.

Before generating anything, read the canonical instructions in `../../../SKILL.md`. Then load only the supporting files needed for the requested sample from:

- `../../../config/stack-matrix.yaml`
- `../../../contracts/`
- `../../../guides/`
- `../../../test-data/`

Follow the root `SKILL.md` workflow exactly. Ask for a destination local path or repository link if one is not already provided, then generate the self-contained sample project under `<destination>/<sample-id>/`. Install dependencies and run the documented tests from inside that generated sample folder before reporting completion.

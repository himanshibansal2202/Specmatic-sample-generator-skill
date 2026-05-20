---
name: generate-specmatic-sample
description: Generate, regression-test, or maintain Specmatic v3 sample projects. Use when the user wants to create a Backend, BFF, or Frontend sample, run regression across all supported combinations, or maintain existing samples. Triggers on "generate a specmatic sample", "run regression", "maintain samples", or "scaffold a backend REST, gRPC, GraphQL, AsyncAPI, or SOAP service with contract tests".
---

# Generate Specmatic Sample

This is the Claude Code project-skill entrypoint for the portable skill stored at the repository root.

Before generating anything, read the canonical instructions in `../../../SKILL.md`. Then load only the supporting files needed for the requested sample from:

- `../../../contracts/`
- `../../../guides/`
- `../../../test-data/`

Follow the root `SKILL.md` workflow exactly. Generate the sample project in the current working directory unless the user provides a target directory. Install dependencies and run the documented tests until they pass before reporting completion.

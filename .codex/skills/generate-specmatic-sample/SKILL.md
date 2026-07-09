---
name: generate-specmatic-sample
description: Generate or maintain working Specmatic sample projects using the current supported Specmatic configuration schema for a given tech stack and protocol. Use when the user wants to create a Backend, BFF, or Frontend sample that demonstrates Specmatic contract testing, or when they want to update existing samples to align with contract changes, dependency upgrades, or Specmatic runtime/configuration updates. Triggers on requests like "generate a specmatic sample", "create a sample project for Java Spring Boot", "scaffold a backend REST, gRPC, GraphQL, AsyncAPI, or SOAP service with contract tests", "maintain my specmatic samples", or "update my samples repo".
---

# Specmatic Sample Skill

This is the Codex project-skill entrypoint for the portable skill stored at the repository root.

Before doing anything, read the canonical instructions in `../../../SKILL.md`. Then load only the supporting files needed for the requested operation from:

- `../../../guides/`
- `../../../test-data/`

Follow the root `SKILL.md` workflow exactly. Start by asking the user whether they want to generate or maintain, then follow the corresponding workflow section.

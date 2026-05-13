Use the `generate-specmatic-sample` skill. If the skill is not already loaded, read `.claude/skills/generate-specmatic-sample/SKILL.md`, then follow the root `SKILL.md` instructions it points to.

If $ARGUMENTS is provided, parse it as the target combination (e.g., "backend rest express javascript in-memory"). Otherwise, ask the user interactively for each input.

Generate the sample project in the current working directory. Install dependencies and run tests until they pass. Report the final test results.

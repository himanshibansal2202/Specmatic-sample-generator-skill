# Generate Specmatic Sample (Kiro Invocation)

To use this skill in Kiro, start a new session and say:

```
Read all files in /path/to/generate-specmatic-sample/ (SKILL.md, references/*, assets/*). 
Then follow the SKILL.md workflow to generate a Backend REST sample using JavaScript + Express + in-memory. 
Generate files in the current directory, install deps, and run tests until green.
```

Or for the interactive flow:

```
Read all files in /path/to/generate-specmatic-sample/ (SKILL.md, references/*, assets/*).
Then follow the SKILL.md workflow — ask me for the inputs interactively.
```

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

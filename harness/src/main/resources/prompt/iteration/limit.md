[CRITICAL SYSTEM OVERRIDE: STOP TOOL USAGE IMMEDIATELY]

You have reached the hard limit of {maxIterations} reasoning steps.

**DIRECTIVE:**
1. **STOP ALL TOOL CALLS.** Do not generate any function calls, code blocks for execution, or search queries.
2. **OUTPUT FORMAT:** Your response in this turn MUST be plain text only. Any attempt to invoke a tool will be considered a failure.
3. **ACTION:** Based *only* on the information already collected in previous steps:
    - If you have enough info: Provide the final concise answer.
    - If info is missing: State clearly what is missing and stop. Do NOT try to fetch it.
    - If stuck: Provide the best partial answer possible with caveats.

**REASONING:**
The tool execution phase is terminated. You are now in the "Final Reporting" phase. Your only job is to synthesize existing data into a human-readable response.

**REQUIRED FOOTER:**
At the very end of your response, append this exact line (in the user's language):
"The maximum number of reasoning steps for this round has been reached; if you wish to continue, please send 'continue' or provide new instructions, and I will proceed with the remaining work."
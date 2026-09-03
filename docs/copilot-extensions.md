# Copilot Extensions (companion plugin)

This is an **optional Eclipse plugin** that sits next to official GitHub Copilot. Extra MCP integrations stay here so they do not have to be accepted into the Microsoft plugin.

## Why a separate plugin?

GitHub Copilot for Eclipse already exposes an MCP contribution API (`com.microsoft.copilot.eclipse.ui.mcpRegistration` / `IMcpRegistrationProvider`). Organization policy can also disable that contribution point. Shipping extra servers, workspace discovery, and fallback behavior inside the official feature is unlikely to land upstream. This companion feature can be installed on its own.

## What it does

- Discovers MCP configs from Eclipse projects:
  - `.vscode/mcp.json`
  - `.cursor/mcp.json`
  - `mcp.json`
  - `.github/mcp.json`
- Optionally registers the GitHub remote MCP server (`https://api.githubcopilot.com/mcp/`)
- Accepts extra MCP JSON on **Window → Preferences → GitHub Copilot → Extensions**
- Contributes those servers through Copilot's official MCP extension point when policy allows it
- When the contribution point is disabled, can merge the same servers into Copilot MCP preferences using the `copilot-ext.` name prefix (user servers are left intact)
- **Refresh Copilot Extensions MCP** reloads the discovered configuration

## Install

1. Install **GitHub Copilot** from this repository's update site (or the official Marketplace build).
2. From the same local/update site, install **Copilot Extensions**.
3. Restart Eclipse.
4. Open **Window → Preferences → GitHub Copilot → Extensions**.

If Copilot prompts you to approve MCP servers from a contributing plugin, approve **Copilot Extensions**.

## Writing another companion

Any Eclipse plugin can contribute MCP servers without forking Copilot:

```xml
<extension point="com.microsoft.copilot.eclipse.ui.mcpRegistration">
  <provider class="com.example.MyMcpRegistrationProvider" id="myProvider"/>
</extension>
```

The class must implement `com.microsoft.copilot.eclipse.ui.extensions.IMcpRegistrationProvider` and return JSON of the form `{"servers":{"name":{...}}}`.

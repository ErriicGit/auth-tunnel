# Auth Tunnel (Minecraft Mod)

Latest release: [Get the newest release here](https://github.com/erriicgit/auth-tunnel/releases)  

If you are looking for the server software, please check out [auth-tunnel-server](https://github.com/erriicgit/auth-tunnel-server)

## What is this?

Auth Tunnel is a Minecraft client mod that adds an account selector for remote accounts hosted on an AuthTunnel server. Instead of storing multiple accounts locally, this mod lets you pick from accounts provided by a running AuthTunnel server.
> [!WARNING]
> This project is in very early development and is expected to be unstable and full of bugs. Use at your own risk. Feel free to report bugs by opening an issue.

## Screenshots
![screenshot_1.png](screenshot_2.png)
![screenshot_2.png](screenshot_1.png)

## Requirements

- A compatible Minecraft client (Minecraft 26.2, Fabric Loader 0.19.3)
- An AuthTunnel server running and reachable from the client. (I will link the AuthTunnel server repository and releases here later.)

## Quick start

1. Install the mod into your Minecraft `mods` folder.
2. Start an AuthTunnel server and ensure it is reachable from your client machine.
3. Launch Minecraft. Open the Auth Tunnel account selector in the mod options or the account menu and add the remote server address as well as the api key.
4. Select a remote account from the list and log in.

If you need the server software, see the repository link above.

## Configuration

The mod expects an AuthTunnel server URL and optional credentials (depending on server configuration). Typical settings you may need to provide:

- Server URL (e.g. `auth.example.com`)
- API key or token (if your server requires one)

Configuration can be entered in the mod UI or via the mod configuration file in the Minecraft `config/` directory.

## Troubleshooting

- If accounts don't show up: verify the server URL is correct and reachable from the client machine.
- If login fails: check server logs and ensure any required API keys or tokens are correct.
- Crashes on startup: remove the mod from `mods/` and test; provide crash logs when reporting issues.

## Contributing

Contributions, bug reports and feature requests are welcome. When opening issues or pull requests, please include:

- A clear description of the problem or feature
- Minecraft/mod loader version and mod version
- Steps to reproduce and any relevant logs

## License
This project is licensed under the MIT License. See [License](LICENSE).
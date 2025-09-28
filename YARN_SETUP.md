# Yarn Berry Configuration

## 📁 Yarn Files in Git

### ✅ TO INCLUDE (REQUIRED)
- `.yarnrc.yml` - Yarn Berry configuration (MANDATORY)
- `yarn.lock` - Exact version locking (MANDATORY)

### ❌ TO IGNORE (in .gitignore)
- `.yarn/` - Cache and temporary files
- `.pnp.*` - Plug'n'Play files

## 🔧 Current Configuration

The `.yarnrc.yml` file contains:
```yaml
nodeLinker: node-modules
enableGlobalCache: false
```

This configuration:
- Uses the classic module system (`node_modules/`)
- Disables global cache to avoid conflicts
- Compatible with React Native

## 🚀 Installation

After a `git clone`, run:
```bash
yarn install
```

## ⚠️ Important

**DO NOT** remove `.yarnrc.yml` from the Git repository!
This file is essential for all developers to use the same Yarn configuration.
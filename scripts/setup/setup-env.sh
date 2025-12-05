export HOME=/home/gitlab-runner
export SDKMAN_DIR="$HOME/.sdkman"
export NVM_DIR="$HOME/.nvm"
export XDG_RUNTIME_DIR=/run/user/999
source "$SDKMAN_DIR/bin/sdkman-init.sh"
. "$NVM_DIR/nvm.sh" || true
sdk env install || true
nvm install || true
nvm use || true
corepack enable || true
yarn install
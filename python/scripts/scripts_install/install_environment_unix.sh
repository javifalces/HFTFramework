#!/bin/bash

# ==========================
# Read environment variables from properties file
# ==========================
PROPS_FILE="$(dirname "$0")/environment.properties"
if [ -f "$PROPS_FILE" ]; then
    while IFS='=' read -r key value || [[ -n "$key" ]]; do
        key=$(echo "$key" | xargs)
        if [[ ! "$key" =~ ^#.*$ ]] && [ -n "$key" ]; then
            if [ "$key" = "CUDA_VERSION" ]; then CUDA_VERSION="$value"; fi
            if [ "$key" = "PYTHON_VERSION" ]; then PYTHON_VERSION="$value"; fi
        fi
    done < "$PROPS_FILE"
    echo "Loaded configuration: CUDA_VERSION=$CUDA_VERSION, PYTHON_VERSION=$PYTHON_VERSION"
else
    echo "Warning: properties file not found. Using default values."
    CUDA_VERSION="cu128"
    PYTHON_VERSION="3.10"
fi

# Start timer
START_TIME=$(date +%s)

echo "=============================================="
echo "Installing dependencies for market making framework"
echo "=============================================="

echo "[Step 1] Checking for CUDA installation..."
if command -v nvcc &> /dev/null; then
    echo "CUDA detected! Will install with CUDA support."
    CUDA_AVAILABLE=true
else
    echo "CUDA not detected. Will install CPU-only packages."
    CUDA_AVAILABLE=false
fi

# ==========================
# Confirmation step after GPU detection
# ==========================
echo ""
if [ "$CUDA_AVAILABLE" = true ]; then
    INSTALL_TYPE="CUDA"
    read -p "Proceed with CUDA support installation? (Y/N) " USER_CONFIRM
else
    INSTALL_TYPE="CPU-ONLY"
    read -p "Proceed with CPU-only installation? (Y/N) " USER_CONFIRM
fi

if [ "${USER_CONFIRM^^}" != "Y" ]; then
    echo "Installation cancelled by user."
    exit 1
fi

echo ""

echo "[Step 2] Creating virtual environment with Python $PYTHON_VERSION..."
# Create a virtual environment in the project directory
VENV_DIR="$(dirname "$0")/../../.venv"
echo "Creating virtual environment at $VENV_DIR"
python3 -m venv "$VENV_DIR"
if [ $? -ne 0 ]; then
    echo "Error creating virtual environment. Exiting."
    exit $?
fi

echo "[Step 3] Activating virtual environment..."
source "$VENV_DIR/bin/activate"
if [ $? -ne 0 ]; then
    echo "Error activating virtual environment. Exiting."
    exit $?
fi

echo "Installing uv..."
pip install uv
if [ $? -ne 0 ]; then
    echo "Error installing uv. Exiting."
    exit $?
fi

echo ""

echo "[Step 4] Installing Python packages..."
echo "Installing Jupyter..."
uv pip install jupyter
if [ $? -ne 0 ]; then
    echo "Error installing jupyter. Exiting."
    exit $?
fi

echo "Installing PyTorch packages..."
if [ "$CUDA_AVAILABLE" = true ]; then
    echo "Installing PyTorch with CUDA support..."
    uv pip install torch torchvision torchaudio --index-url "https://download.pytorch.org/whl/$CUDA_VERSION"
    if [ $? -ne 0 ]; then
        echo "Error installing PyTorch with CUDA. Exiting."
        exit $?
    fi
else
    echo "Installing PyTorch with CPU support"
    uv pip install torch torchvision torchaudio
    if [ $? -ne 0 ]; then
        echo "Error installing PyTorch with CPU. Exiting."
        exit $?
    fi
fi

echo ""
echo "[Step 5] Installing JAX..."
if [ "$CUDA_AVAILABLE" = true ]; then
    echo "Installing JAX with CUDA 12 support (required by sbx-rl for GPU acceleration)..."
    uv pip install "jax[cuda12]"
    if [ $? -ne 0 ]; then
        echo "Error installing JAX with CUDA. Exiting."
        exit $?
    fi
else
    echo "Installing JAX with CPU support..."
    uv pip install "jax[cpu]"
    if [ $? -ne 0 ]; then
        echo "Error installing JAX with CPU. Exiting."
        exit $?
    fi
fi

echo ""
echo "[Step 6] Installing remaining requirements from requirements.txt..."
cd "$(dirname "$0")/../.."
uv pip install -r requirements.txt
if [ $? -ne 0 ]; then
    echo "Error installing requirements. Exiting."
    exit $?
fi

if [ "$CUDA_AVAILABLE" = true ]; then
    echo ""
    echo "[Step 7] Installing GPU-specific extras from requirements-gpu.txt..."
    uv pip install -r requirements-gpu.txt
    if [ $? -ne 0 ]; then
        echo "Error installing GPU requirements. Exiting."
        exit $?
    fi
fi

echo ""
echo "=============================================="
echo "Installation completed successfully!"
echo ""

# Show total elapsed time
END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))
HOURS=$((ELAPSED / 3600))
MINUTES=$(( (ELAPSED % 3600) / 60 ))
SECONDS=$((ELAPSED % 60))
printf "Total time elapsed: %02d:%02d:%02d\n" $HOURS $MINUTES $SECONDS

echo "To activate the environment, run: source $VENV_DIR/bin/activate"
echo "=============================================="
echo ""
echo "Press Enter to exit..."
read

# ==========================
# Read environment variables from properties file
# ==========================
$propsFile = "$PSScriptRoot\environment.properties"
if (Test-Path $propsFile) {
    Get-Content $propsFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and !$line.StartsWith('#')) {
            $key, $value = $line.Split('=', 2)
            if ($key -eq "CUDA_VERSION") { $CUDA_VERSION = $value }
            if ($key -eq "PYTHON_VERSION") { $PYTHON_VERSION = $value }
        }
    }
    Write-Host "Loaded configuration: CUDA_VERSION=$CUDA_VERSION, PYTHON_VERSION=$PYTHON_VERSION"
} else {
    Write-Host "Warning: properties file not found. Using default values."
    $CUDA_VERSION = "cu128"
    $PYTHON_VERSION = "3.10"
}

# Start timer
$startTime = Get-Date

Write-Host "=============================================="
Write-Host "Installing dependencies for market making framework"
Write-Host "=============================================="

Write-Host "[Step 1] Checking for CUDA installation..."
$nvcc = Get-Command nvcc -ErrorAction SilentlyContinue
if ($nvcc) {
    Write-Host "CUDA detected! Will install with CUDA support."
    $CUDA_AVAILABLE = $true
} else {
    Write-Host "CUDA not detected. Will install CPU-only packages."
    $CUDA_AVAILABLE = $false
}

# ==========================
# Confirmation step after GPU detection
# ==========================
Write-Host ""
if ($CUDA_AVAILABLE) {
    $INSTALL_TYPE = "CUDA"
    $USER_CONFIRM = Read-Host "Proceed with CUDA support installation? (Y/N)"
} else {
    $INSTALL_TYPE = "CPU-ONLY"
    $USER_CONFIRM = Read-Host "Proceed with CPU-only installation? (Y/N)"
}
if ($USER_CONFIRM.ToUpper() -ne "Y") {
    Write-Host "Installation cancelled by user."
    exit 1
}

Write-Host ""

Write-Host "[Step 2] Creating conda environment 'lambda2' with Python $PYTHON_VERSION..."
if ($CUDA_AVAILABLE) {
    Write-Host "Installing with CUDA support..."
    conda create -y -n lambda2 "python=$PYTHON_VERSION" jupyter cudatoolkit -c conda-forge
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error creating conda environment. Exiting."
        exit $LASTEXITCODE
    }
} else {
    Write-Host "Installing CPU-only version..."
    conda create -y -n lambda2 "python=$PYTHON_VERSION" jupyter
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error creating conda environment. Exiting."
        exit $LASTEXITCODE
    }
}

Write-Host "[Step 3] Activating conda environment 'lambda2'..."
conda activate lambda2
pip install uv
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error activating conda environment. Exiting."
    exit $LASTEXITCODE
}

Write-Host ""

Write-Host "[Step 4] Installing PyTorch packages..."
if ($CUDA_AVAILABLE) {
    Write-Host "Installing PyTorch with CUDA support..."
    uv pip install torch torchvision torchaudio --index-url "https://download.pytorch.org/whl/$CUDA_VERSION"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error installing PyTorch with CUDA. Exiting."
        exit $LASTEXITCODE
    }
} else {
    Write-Host "PyTorch CPU-only version already installed during environment creation."
    Write-Host "Installing PyTorch with CPU support"
    uv pip install torch torchvision torchaudio
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error installing PyTorch with CPU. Exiting."
        exit $LASTEXITCODE
    }
}

Write-Host ""
Write-Host "[Step 5] Installing JAX..."
if ($CUDA_AVAILABLE) {
    Write-Host "Installing JAX with CUDA 12 support (required by sbx-rl for GPU acceleration)..."
    uv pip install "jax[cuda12]"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error installing JAX with CUDA. Exiting."
        exit $LASTEXITCODE
    }
} else {
    Write-Host "Installing JAX with CPU support..."
    uv pip install "jax[cpu]"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error installing JAX with CPU. Exiting."
        exit $LASTEXITCODE
    }
}

Write-Host ""
Write-Host "[Step 6] Installing remaining requirements from requirements.txt..."
Set-Location "$PSScriptRoot\..\.."
uv pip install -r requirements.txt
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error installing requirements. Exiting."
    exit $LASTEXITCODE
}

if ($CUDA_AVAILABLE) {
    Write-Host ""
    Write-Host "[Step 7] Installing GPU-specific extras from requirements-gpu.txt..."
    uv pip install -r requirements-gpu.txt
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error installing GPU requirements. Exiting."
        exit $LASTEXITCODE
    }
}

Write-Host ""
Write-Host "=============================================="
Write-Host "Installation completed successfully!"
Write-Host ""

# Show total elapsed time
$endTime = Get-Date
$elapsed = $endTime - $startTime
Write-Host ("Total time elapsed: {0:hh\:mm\:ss}" -f $elapsed)

Write-Host "To activate the environment, run: conda activate lambda2"
Write-Host "=============================================="
Write-Host ""
Write-Host "Press any key to exit..."
[void][System.Console]::ReadKey($true)

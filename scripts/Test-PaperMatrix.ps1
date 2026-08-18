[CmdletBinding()]
param(
    [string] $PluginJar = "",
    [int] $StartupTimeoutSeconds = 180,
    [switch] $KeepWorkDirectories
)

$ErrorActionPreference = "Stop"

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$targetRoot = [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot "target"))
if ([string]::IsNullOrWhiteSpace($PluginJar)) {
    $PluginJar = Join-Path $targetRoot "EZPortalRedirect-1.0.0.jar"
}
$PluginJar = (Resolve-Path -LiteralPath $PluginJar).Path

$matrix = @(
    @{
        Version = "1.13.2"
        Java = "C:\Program Files\Java\jre1.8.0_341\bin\java.exe"
        NoGuiArgument = "nogui"
    },
    @{
        Version = "1.21.11"
        Java = "C:\Program Files\Java\jdk-21\bin\java.exe"
        NoGuiArgument = "--nogui"
    },
    @{
        Version = "26.2"
        Java = "C:\Program Files\Java\jdk-25\bin\java.exe"
        NoGuiArgument = "--nogui"
    }
)

$headers = @{
    "User-Agent" = "EZPortalRedirect-test/1.0.0 (myztic_dev)"
}
$runRoot = Join-Path $targetRoot ("paper-smoke-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null
$results = [System.Collections.Generic.List[object]]::new()
$allPassed = $false

try {
    foreach ($entry in $matrix) {
        $version = $entry.Version
        $javaExecutable = $entry.Java
        $noGuiArgument = $entry.NoGuiArgument
        if (-not (Test-Path -LiteralPath $javaExecutable -PathType Leaf)) {
            throw "Java executable not found for Paper $version`: $javaExecutable"
        }

        Write-Host "Resolving latest stable Paper $version build..."
        $builds = Invoke-RestMethod `
            -Headers $headers `
            -Uri "https://fill.papermc.io/v3/projects/paper/versions/$version/builds"
        $build = $builds | Where-Object channel -eq "STABLE" | Select-Object -First 1
        if ($null -eq $build) {
            throw "Paper $version has no stable build in the downloads service."
        }

        $download = $build.downloads."server:default"
        if ($null -eq $download -or [string]::IsNullOrWhiteSpace($download.url)) {
            throw "Paper $version build $($build.id) has no default server download."
        }

        $serverDirectory = Join-Path $runRoot $version
        $pluginsDirectory = Join-Path $serverDirectory "plugins"
        New-Item -ItemType Directory -Force -Path $pluginsDirectory | Out-Null
        $serverJar = Join-Path $serverDirectory "paper.jar"
        Write-Host "Downloading Paper $version build $($build.id)..."
        Invoke-WebRequest -UseBasicParsing -Headers $headers -Uri $download.url -OutFile $serverJar
        Copy-Item -LiteralPath $PluginJar -Destination $pluginsDirectory

        [System.IO.File]::WriteAllText(
            (Join-Path $serverDirectory "eula.txt"),
            "eula=true`r`n")
        [System.IO.File]::WriteAllText(
            (Join-Path $serverDirectory "server.properties"),
            "online-mode=false`r`nserver-port=0`r`nview-distance=2`r`nsimulation-distance=2`r`n")

        $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
        $startInfo.FileName = $javaExecutable
        $startInfo.Arguments = "-Xms512M -Xmx1024M -jar paper.jar $noGuiArgument"
        $startInfo.WorkingDirectory = $serverDirectory
        $startInfo.UseShellExecute = $false
        $startInfo.CreateNoWindow = $true
        $startInfo.RedirectStandardInput = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true

        $process = [System.Diagnostics.Process]::new()
        $process.StartInfo = $startInfo
        Write-Host "Starting Paper $version with $([System.IO.Path]::GetFileName((Split-Path $javaExecutable -Parent)))..."
        if (-not $process.Start()) {
            throw "Could not start Paper $version."
        }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()

        $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
        $startupComplete = $false
        $pluginEnabled = $false
        $latestLog = Join-Path $serverDirectory "logs\latest.log"

        while (-not $process.HasExited -and [DateTime]::UtcNow -lt $deadline) {
            if (Test-Path -LiteralPath $latestLog) {
                $log = Get-Content -Raw -LiteralPath $latestLog -ErrorAction SilentlyContinue
                $startupComplete = $log -match 'Done \([0-9.,]+s\)!'
                $pluginEnabled = $log -match 'EZPortalRedirect enabled successfully!'
                if ($startupComplete -and $pluginEnabled) {
                    break
                }
            }
            Start-Sleep -Milliseconds 500
        }

        if (-not $process.HasExited) {
            $process.StandardInput.WriteLine("stop")
            $process.StandardInput.Flush()
            if (-not $process.WaitForExit(30000)) {
                $process.Kill()
                $process.WaitForExit()
            }
        }

        $stdout = $stdoutTask.GetAwaiter().GetResult()
        $stderr = $stderrTask.GetAwaiter().GetResult()
        [System.IO.File]::WriteAllText((Join-Path $serverDirectory "console.log"), $stdout + $stderr)

        $passed = $startupComplete -and $pluginEnabled -and $process.ExitCode -eq 0
        $results.Add([PSCustomObject]@{
            Version = $version
            Build = $build.id
            Java = (& $javaExecutable -version 2>&1 | Select-Object -First 1).ToString()
            StartupComplete = $startupComplete
            PluginEnabled = $pluginEnabled
            ExitCode = $process.ExitCode
            Passed = $passed
        })

        if (-not $passed) {
            $tail = (($stdout + $stderr) -split "`r?`n" | Select-Object -Last 40) -join "`n"
            throw "Paper $version smoke test failed.`n$tail"
        }
        Write-Host "Paper $version passed."
    }

    $allPassed = $true
}
finally {
    $resultsPath = Join-Path $targetRoot "paper-smoke-results.json"
    $results | ConvertTo-Json | Set-Content -LiteralPath $resultsPath -Encoding UTF8

    if ($allPassed -and -not $KeepWorkDirectories) {
        $resolvedRunRoot = [System.IO.Path]::GetFullPath($runRoot)
        $expectedPrefix = $targetRoot.TrimEnd('\') + '\paper-smoke-'
        if (-not $resolvedRunRoot.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove unexpected smoke-test path: $resolvedRunRoot"
        }
        Remove-Item -LiteralPath $resolvedRunRoot -Recurse -Force
    }
}

$results | Format-Table -AutoSize
Write-Host "Paper smoke-test results: $(Join-Path $targetRoot 'paper-smoke-results.json')"

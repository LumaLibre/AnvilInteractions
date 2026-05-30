@echo off
REM Quick patcher script (Paper-style per-file patches)
REM Usage: patcher.bat <command> <args?>

setlocal enabledelayedexpansion

REM --- Find gradle ---
if exist "gradlew.bat" (
    set "GRADLE=gradlew.bat"
) else (
    where gradle >nul 2>nul
    if !errorlevel! equ 0 (
        set "GRADLE=gradle"
    ) else (
        echo Error: Can't find gradle. Run this from the project root.
        exit /b 1
    )
)

REM --- Dispatch on the first argument ---
set "CMD=%~1"

if /i "%CMD%"=="setup"        goto :setup
if /i "%CMD%"=="init"         goto :setup
if /i "%CMD%"=="fresh"        goto :fresh
if /i "%CMD%"=="status"       goto :status
if /i "%CMD%"=="s"            goto :status
if /i "%CMD%"=="apply"        goto :apply
if /i "%CMD%"=="a"            goto :apply
if /i "%CMD%"=="apply-only"   goto :applyonly
if /i "%CMD%"=="apply-fuzzy"  goto :applyfuzzy
if /i "%CMD%"=="af"           goto :applyfuzzy
if /i "%CMD%"=="rebuild"      goto :rebuild
if /i "%CMD%"=="r"            goto :rebuild
if /i "%CMD%"=="reset"        goto :reset
if /i "%CMD%"=="list"         goto :list
if /i "%CMD%"=="l"            goto :list
if /i "%CMD%"=="inspect"      goto :inspect
if /i "%CMD%"=="i"            goto :inspect
if /i "%CMD%"=="clean"        goto :clean
goto :usage


:setup
    REM Decompile (if needed) + distribute base + apply all file patches.
    echo === Setup ===
    call %GRADLE% setup || exit /b 1
    echo Done!
    goto :eof


:fresh
    echo === Fresh Start ===
    echo This wipes module sources + the decompiled base and rebuilds from scratch.
    set /p "confirm=Continue? [y/N] "
    if /i "!confirm!"=="y" (
        call %GRADLE% cleanDistributedSources cleanGenerated cleanPatchCache || exit /b 1
        call %GRADLE% setup || exit /b 1
        echo Finished!
    )
    goto :eof


:status
    REM Which module files differ from the pristine base.
    call %GRADLE% patchStatus || exit /b 1
    goto :eof


:apply
    REM Reconstruct the working tree: base + every file patch.
    call %GRADLE% applyPatches || exit /b 1
    goto :eof


:applyonly
    REM Overlay patches onto whatever is already in the modules (no re-distribute).
    call %GRADLE% applyFilePatches || exit /b 1
    goto :eof


:applyfuzzy
    REM Same as apply but adds a fuzzy `patch` rung before rejecting hunks.
    call %GRADLE% applyFilePatchesFuzzy || exit /b 1
    goto :eof


:rebuild
    REM Regenerate per-file patches from your current module edits.
    REM This is also how you "finish" a conflict: fix the files, then rebuild.
    call %GRADLE% rebuildFilePatches || exit /b 1
    goto :eof


:reset
    REM Throw away manual module edits, rebuild working tree from base + patches.
    echo This discards uncommitted edits in the module sources.
    set /p "confirm=Continue? [y/N] "
    if /i "!confirm!"=="y" (
        call %GRADLE% resetSources || exit /b 1
    )
    goto :eof


:list
    call %GRADLE% listPatches || exit /b 1
    goto :eof


:inspect
    call %GRADLE% inspectDecompiledStructure || exit /b 1
    goto :eof


:clean
    echo Clean options:
    echo   1^) Module sources
    echo   2^) Generated/decompiled base
    echo   3^) Patch work dirs (.patch-work / .patch-rejects)
    echo   4^) All
    set /p "choice=Choice: "
    if "!choice!"=="1" call %GRADLE% cleanDistributedSources || exit /b 1
    if "!choice!"=="2" call %GRADLE% cleanGenerated || exit /b 1
    if "!choice!"=="3" call %GRADLE% cleanPatchCache || exit /b 1
    if "!choice!"=="4" call %GRADLE% cleanDistributedSources cleanGenerated cleanPatchCache || exit /b 1
    goto :eof


:usage
    echo Patcher commands:
    echo.
    echo   setup, init      - Decompile + distribute base + apply all file patches
    echo   fresh            - Wipe everything and rebuild from scratch
    echo.
    echo   status, s        - Show which module files differ from the base
    echo   apply, a         - Reconstruct module sources = base + all patches
    echo   apply-fuzzy, af  - apply with an extra fuzzy matching rung
    echo   apply-only       - Overlay patches without re-distributing the base
    echo   rebuild, r       - Regenerate per-file patches from current module edits
    echo   reset            - Discard module edits; rebuild tree from base + patches
    echo   list, l          - List the per-file patches
    echo.
    echo   inspect, i       - Show decompiled structure
    echo   clean            - Clean up sources / base / work dirs
    echo.
    echo Conflict workflow (one patch per file, so failures are isolated):
    echo   1^) 'patcher.bat apply' reports files needing attention
    echo        - conflict markers are left directly in the module file
    echo        - rejected hunks are written to .patch-rejects\^<path^>.rej
    echo   2^) Fix those files in your IDE
    echo   3^) 'patcher.bat rebuild' regenerates clean patches from your fixes
    echo.
    echo Edit / add code:
    echo   1^) Edit files in the module src tree (or add new ones)
    echo   2^) 'patcher.bat status' to see what changed
    echo   3^) 'patcher.bat rebuild' to update the patch tree
    echo.
    echo Examples:
    echo   patcher.bat setup
    echo   patcher.bat status
    echo   patcher.bat rebuild
    goto :eof
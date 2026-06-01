@echo off
echo ===================================================
echo   Mendorong Perubahan MiniBookLibrary ke GitHub
echo ===================================================
echo.

:: Inisialisasi git jika belum ada
if not exist .git (
    echo Menginisialisasi repositori Git lokal...
    git init
)

:: Atur atau perbarui URL remote
echo Mengonfigurasi remote GitHub...
git remote remove origin >nul 2>&1
git remote add origin https://github.com/SultanZhalifa/MiniBookLibrary.git

:: Tambahkan file dan commit
echo Menambahkan perubahan file...
git add .

echo Membuat commit...
git commit -m "Modernisasi total UI/UX ke Material 3 dan perbaikan build error"

:: Push ke GitHub pada branch master
echo Mendorong (push) perubahan ke GitHub (branch: master)...
git branch -M master
git push -u origin master

echo.
echo ===================================================
echo Selesai! Jika berhasil, silakan periksa GitHub Anda.
echo ===================================================
pause

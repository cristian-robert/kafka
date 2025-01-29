import subprocess
import sys

def install_dependencies():
    subprocess.check_call([sys.executable, '-m', 'pip', 'install', 
                          'requests', 'pandas', 'pyinstaller'])

if __name__ == "__main__":
    install_dependencies()
    subprocess.check_call(['pyinstaller', '--onefile', 'request_script.py'])
    print("Executable created in 'dist' folder")

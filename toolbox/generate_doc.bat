@echo off
::删除旧的apidoc
Rd /s /q "build/outputs/aar/doc/apidoc" >nul 2>nul
::创建doc文件夹
mkdir "build/outputs/aar/doc/apidoc"
@echo.    JavaDoc Generate Started..
javadoc.exe -windowtitle "MyToolBox Guide" -bottom "Copyright &copy; 2019-2028 threshold. All rights reserved." -protected -splitindex -encoding utf-8 -charset utf-8 -d build/outputs/aar/doc/apidoc @javadoc_args.txt -Xdoclint:none
@echo.    JavaDoc Generate Complete..
@echo.    Press any key to exit.
@pause>nul
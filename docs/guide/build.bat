@echo off
if "%1" == "" goto skip
copy en\chap%1.xml en\temp.xml
ant lang.dochtml.one
del en\temp.xml
exit

:skip
echo need chapter number
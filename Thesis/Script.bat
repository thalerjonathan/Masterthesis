@echo off
set mode=batchmode
echo Start

echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 
echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 
echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 

echo bibtex
bibtex -terse build/Masterthesis
bibtex -terse build/Masterthesis

echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 
echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 
echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 

cd build\
echo Glossaries
makeglossaries -q Masterthesis
echo Glossaries
makeglossaries -q Masterthesis
cd ..

echo PDFLatex
pdflatex --output-directory=build  -quiet -synctex=1 -interaction=%mode% * 
echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 
echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 

echo bibtex
bibtex -terse build/Masterthesis 
bibtex -terse build/Masterthesis

echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 
echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 
echo PDFLatex
pdflatex --output-directory=build -quiet -synctex=1 -interaction=%mode% * 

echo Done

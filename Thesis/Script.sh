#!/bin/bash
# Proper header for a Bash script.

# mode = nonstopmode
mode=batchmode
echo Start

echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null
echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null
echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null

echo Bibtex
bibtex -terse build/*.aux 
echo Bibtex
bibtex -terse build/*.aux

echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null
echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null
echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null

echo Glossaries
makeglossaries -q build/Masterthesis
echo Glossaries
makeglossaries -q build/Masterthesis

echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null
echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null
echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null

echo Bibtex
bibtex -terse build/*.aux 
echo Bibtex
bibtex -terse build/*.aux

echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null
echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null
echo PDFLatex
pdflatex --output-directory=build -synctex=1 -interaction=$mode * >/dev/null

evince build/*.pdf

echo Done

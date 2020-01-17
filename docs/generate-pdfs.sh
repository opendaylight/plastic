# This is not part of production but is a simple way for developers to quickly
# generate PDFs to check formatting.

for f in *.rst
do
    b=`basename "$f" .rst`
    rst2pdf --stylesheets=simple-modified.style "$f" "$b.pdf"
done

open *.pdf


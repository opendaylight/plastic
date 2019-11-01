for f in *.rst
do
    b=`basename "$f" .rst`
    rst2pdf --stylesheets=simple-modified.style "$f" "$b.pdf"
done

open *.pdf


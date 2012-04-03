#!/bin/sh
printf "Generating images:"
for x in {0..99} ; do
  printf " %s" "$x"
  line=$(tail -n $((100 - $x)) rainbow-colors.txt | head -1)
  convert -background "$line" -fill black -size 250x250 -gravity center label:"$x" $x.png
done
printf "\n"

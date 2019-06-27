# script for plotting EC boxplot results
# on gnuplot, type "load <script file>"

# data/plot directory
dir="test-final"

# EPS output
# set term postscript enhanced color
# set output dir."/boxplot.eps"
# set output dir."/boxplot-min.eps"

# PDF output
set term pdf enhanced color
set output dir."/boxplot.pdf"
# set output dir."/boxplot-min.pdf"

set title "Desirability Coefficients found from the EC runs"
# set title "Desirability Coefficients found from the EC runs (focuns on min values)"
set xlabel "Desirability Factors"
set ylabel "Coefficient Values"

set boxwidth 0.5 absolute
set xrange [ 0.0000 : 5.0000 ] noreverse nowriteback

# for normal boxplot
set yrange [ -1.0000 : 1.0000 ] noreverse nowriteback
# set yrange [ -0.6000 : 0.5000 ] noreverse nowriteback

# for boxplot-min
# set yrange [ 0.00000 : 0.0010 ] noreverse nowriteback
# set yrange [ 0.00000 : 0.0010 ] noreverse nowriteback

set border 2
set xtics ("Temperature" 1, "Elevation" 2, "River Dist." 3, "Port Dist." 4) scale 0.0
set xtics nomirror
set ytics nomirror

plot dir."/boxdata.stat" using 1:3:2:6:5 with candlesticks lt 3 lw 2 title 'Quartiles' whiskerbars 0.5, \
''                 using 1:4:4:4:4 with candlesticks lt -1 lw 2 title 'Median', \
''                 using 1:7:7:7:7 with candlesticks lt 4 lw 2 title 'Mean'

set term wxt
plot dir."/boxdata.stat" using 1:3:2:6:5 with candlesticks lt 3 lw 2 title 'Quartiles' whiskerbars 0.5, \
''                 using 1:4:4:4:4 with candlesticks lt -1 lw 2 title 'Median', \
''                 using 1:7:7:7:7 with candlesticks lt 4 lw 2 title 'Mean'

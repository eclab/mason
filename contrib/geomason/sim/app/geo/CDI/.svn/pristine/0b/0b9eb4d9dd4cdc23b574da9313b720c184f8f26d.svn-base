set dgrid3d 100,100
set hidden3d
set terminal pdf
set output 'orig-terrain.pdf'
splot 'orig-terrain.dat' with lines title 'original terrain'
do for [i=0:10] {
	outfile = sprintf("smooth-terrain-%d.pdf", i)
	set output outfile
	splot 'smooth-terrain-'.i.'.dat' with lines title 'smoothing phase-'.i
}
set terminal wxt

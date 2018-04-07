# plots.r
#
# I use two R scripts to do the plotting.
#    results.r - reads in the .csv files
#    plots.r   - plots the data
#
# To execute these, type the following at the R command line:
#   > source("results.r")
#   > source("plots.r")
#
# To exit R, type ctrl-d.  R can retain data from one execution to the next.
# When you exit R you are asked if you want to keep your data.  I recommend
# that you say yes, at least the first time.  Then in subsequent calls, you
# can choose to only execute the plots.r script.  This makes it easier to
# modify and test changes to your plots.  When you have new data, be sure to
# execute the results.r file first, and keep the data when you exit.



png(filename ="HFconflicts.png", width = 640, height = 480,
    pointsize = 16, bg = "white")

boxplot(HFconflicts ~ Step, subset(monthData, Step < 12*7), col="blue",
        xlab = "months", ylab="Average daily incidents")

#dev.off()



png(filename ="HHconflicts.png", width = 640, height = 480,
    pointsize = 16, bg = "white")

boxplot(HHconflicts ~ Step, subset(monthData, Step < 12*7), col="blue",
        xlab = "months", ylab="Average daily incidents")

#dev.off()



png(filename ="herders.png", width = 640, height = 480,
    pointsize = 16, bg = "white")

boxplot(NumHerders ~ Step, subset(monthData, Step < 12*7), col="blue",
        xlab = "months", ylab="Number of families")

#dev.off()



png(filename ="animals.png", width = 640, height = 480,
    pointsize = 16, bg = "white")

boxplot(NumAnimals ~ Step, subset(monthData, Step < 12*7), col="blue",
        xlab = "months", ylab="Total number of livestock")

#dev.off()




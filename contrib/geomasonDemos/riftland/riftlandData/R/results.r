# results.r
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


monthlySummary <- function(dailyData)
{
    monthlyData <- NULL
    totalDays <- dim(dailyData)[1]

    dayOfMonth <- 1
    for (day in 1:totalDays)
    {
        if (dayOfMonth == 1)
            thisMonth <- dailyData[day,]
        else
            thisMonth <- thisMonth + dailyData[day,]

        if (dayOfMonth == 30 || day == totalDays)
        {
            thisMonth <- thisMonth / dayOfMonth
            thisMonth[2] <- day/30        # Don't average the days/steps
            monthlyData <- rbind(monthlyData, thisMonth)
            dayOfMonth = 1
        }
        else
            dayOfMonth <- dayOfMonth + 1
    }

    print(monthlyData)
    monthlySummary <- monthlyData
}


data <- NULL;
monthData <- NULL;
for (file in list.files(pattern="\\.csv$"))
{
    print(file)
    newData <- read.csv(file)
    data <- rbind(data, newData)
    monthData <- rbind(monthData, monthlySummary(newData))
}

#print("Set factor")
#data$Step <- factor(data$Step)
#print(data$Step)

#print("plot")
#boxplot(NumHerders ~ Step, monthData, col="gray")


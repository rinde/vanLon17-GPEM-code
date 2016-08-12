library("ggplot2")
library("data.table")
best.stats <- data.table(read.csv("~/Downloads/best-stats.csv"))

pp <- ggplot(best.stats, aes_string(x="generation", y="cost_per_parcel")) +
  theme_bw(base_size=8) +
  geom_line() +
  ylim(0,NA) +
  theme(axis.text = element_text(size=6)) +
  theme(legend.key=element_rect(fill=NA))

show(pp)
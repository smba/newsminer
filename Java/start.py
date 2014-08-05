import sched, time
import os

s = sched.scheduler(time.time, time.sleep)
TIMESTEP = 60*60*6 #every 6 hours
def startNewsminer(sc):
        print "Starting newsminer"
        os.system("nohup java -jar newsminer.jar foo bar &")
        sc.enter(TIMESTEP, 1, startNewsMiner, (sc,1))
s.enter(TIMESTEP, 1, startNewsMiner, (sc,))
s.run()

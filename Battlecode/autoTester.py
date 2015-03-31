'''
Auto testing script for Battlecode 2014 team069
this requires this file and "antFile.bat" both in the root battlecode directory
antFile.bat will need to be adjusted to match your system
change the first line to set your JAVA_HOME variable to point to your java sdk
change the first line to call ant file, wherever you have ant installed,
I was super lazy so I just linked to the ant that came with the eclipse package.

This script plays one player against a set of opponents on some maps, change
the variables below to specify the test. Then run the script.
After the simulations are done the replays and logs will be dumped into /auto_tester_replays/
and the script will print out the results
'''
import os
from string import Template
import re
import time
import datetime

#Change this stuff
team = 'mchangMicro1'
opponents = ['mchangMicro10Amove']
maplist = ["rushlane"]

ts = time.time()
st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d_%H-%M-%S')
maps = ",".join(maplist)
fileTemplate = Template("# Match server settings\n\
bc.server.throttle=yield\n\
bc.server.throttle-count=50\n\
\n\
# Game engine settings\n\
bc.engine.debug-methods=false\n\
bc.engine.silence-a=false\n\
bc.engine.silence-b=false\n\
bc.engine.gc=false\n\
bc.engine.gc-rounds=50\n\
bc.engine.upkeep=true\n\
bc.engine.breakpoints=false\n\
bc.engine.bytecodes-used=true\n\
\n\
# Client settings\n\
bc.client.opengl=false\n\
bc.client.use-models=true\n\
bc.client.renderprefs2d=\n\
bc.client.renderprefs3d=\n\
bc.client.sound-on=true\n\
bc.client.check-updates=true\n\
bc.client.viewer-delay=50\n\
\n\
# Headless settings - for \"ant file\"\n\
bc.game.maps=$maps\n\
bc.game.team-a=$p1\n\
bc.game.team-b=$p2\n\
bc.server.save-file=$out\n\
\n\
# Transcriber settings\n\
bc.server.transcribe-input=matches\\match.rms\n\
bc.server.transcribe-output=matches\\transcribed.txt\n")
print "Beginning testing {0} against {1} on {2}".format(team,opponents,maplist)
for o in opponents:
    out = "auto_tester_replays/{0}_vs_{1}_{2}.rms".format(team,o,st)
    log = "auto_tester_replays/{0}_vs_{1}_{2}.log".format(team,o,st)
    text = fileTemplate.substitute(maps=maps,p1=team,p2=o,out=out)
    bc = open('bc.conf', 'w')
    bc.write(text)
    bc.close()
    os.system("antFile.bat > {0}".format(log))
    res= open(log,'r')
    results = res.read()
    res.close()
    winners = re.findall(r'(\S*) \(.\) wins',results)
    wins = 0;
    for i in range(0,len(maplist)):
        print winners[i] + " wins on " + maplist[i]
        if(winners[i] == team):
            wins += 1
    print "{0} went {1}/{2} against {3}".format(team,wins,len(winners),o)

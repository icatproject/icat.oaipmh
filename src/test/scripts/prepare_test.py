#!/usr/bin/env python3
from __future__ import print_function
import sys
import os
from string import Template
import tempfile
from filecmp import cmp
import glob
import shutil
from zipfile import ZipFile
import subprocess

if len(sys.argv) != 8:
    raise RuntimeError("Wrong number of arguments")

resourceDir = sys.argv[1]
propFile = sys.argv[2]
dataFile = sys.argv[3]
home = sys.argv[4]
containerHome = sys.argv[5]
icatUrl = sys.argv[6]
icatAuth = sys.argv[7]

resourceDirAbs = os.path.abspath(resourceDir)

subst = dict(os.environ)
subst['HOME'] = home

try:
    tmpf = tempfile.NamedTemporaryFile(delete=False)
    name = tmpf.name
    propFilePath = "%s/%s" % (resourceDir, propFile)
    with open(name, "wt") as f:
        with open(propFilePath, "rt") as s:
            t = Template(s.read()).substitute(subst)
            print(t, end="", file=f)
        print("dataPath = %s/%s" % (resourceDirAbs, dataFile), file=f)
        print("requestUrl = %s/oaipmh/request" % icatUrl, file=f)
        print("icat.auth = %s" % icatAuth, file=f)
        print("icat.url = %s" % icatUrl, file=f)
        print("oai_dc.xslt = %s/oai_dc.xslt" % resourceDirAbs, file=f)
        print("oai_datacite.xslt = %s/oai_datacite.xslt" % resourceDirAbs, file=f)
    print("Installing with %s and %s" % (propFile, dataFile))
    shutil.copy(name, "src/test/install/run.properties")
finally:
    os.remove(name)

for f in glob.glob("src/test/install/*.war"):
    os.remove(f)

with open("src/test/install/setup.properties", "wt") as f:
    print("secure         = true", file=f)
    print("container      = Glassfish", file=f)
    print("home           = %s" % containerHome, file=f)
    print("port           = 4848", file=f)

with open("src/test/install/run.properties.example", "wt") as f:
    pass

shutil.copy(glob.glob("target/icat.oaipmh-*.war")[0], "src/test/install/")
shutil.copy("src/main/scripts/setup", "src/test/install/")

with ZipFile(glob.glob("target/icat.oaipmh-*-distro.zip")[0]) as z:
    with open("src/test/install/setup_utils.py", "wb") as f:
        f.write(z.read("icat.oaipmh/setup_utils.py"))

with open("src/main/resources/logback.xml", "rt") as s:
    with open("src/test/install/logback.xml", "wt") as f:
        t = Template(s.read()).substitute(subst)
        print(t, end="", file=f)

p = subprocess.Popen(["./setup", "install"], cwd="src/test/install")
p.wait()

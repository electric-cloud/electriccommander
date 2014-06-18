# What is this?

This is a tool that will allow you to clean up stalled jobs in ElectricCommander (these are jobs that are not abortable using the GUI or ```ectool```). 

In addition to forcing the job to complete, diagnostic output will be produced that should be forwarded to Electric Cloud Support for further analysis.

# How do I use it?

The tool is packaged as a single jar file with all required classes packaged in the jar file. Well, with one exception: if you use MySQL, due to the GPL you will need to supply the MySQL JDBC drivers yourself.

The intent is that this tool is run from the ElectricCommander server itself, as access to the database.properties and passkey files is required. While those files could be made available on another host, the most practical approach is to run the command on the server itself.

The command line options:
    
    java -jar CleanupStalledJob-jar-with-dependencies.jar --help
    usage: CleanupStalledJob
        --database-properties <filename>   Path to the database.properties file to use. Defaults to
                                           conf/database.properties, or to the value of the
                                           'COMMANDER_DATABASE_PROPERTIES' system property or
                                           environment variable.
        --help                             Output command line help.
        --jobId <integer-or-uuid>          The primary key of the job to clean up.
        --output <filename>                The name of a file to which output will be written. If the
                                           file exists, it will be overwritten. If not supplied, the
                                           output will be written to 'job-<jobId>.txt'
        --passkey <filename>               Path to the passkey file to use. Defaults to conf/passkey, or
                                           to the value of the 'COMMANDER_PASSWORD_KEYFILE' system
                                           property of environment variable.
    
The tool connects directly to the database (as opposed to talking to the ElectricCommander server). Thus it needs to know how to connect to the database. This information is provided in Commander's database.properties file, which must be provided. To decrypt the database password for use, the Commander passkey file must also be provided.

Finally, the primary key of the job to be cleaned up must be provided. For pre-5.0 versions of Commander, this will be an integer. For 5.0 and later, this will be a UUID.

Diagnostic output (mostly, the data from the job itself) will be collected into the output file, which should be forwarded to Electric Cloud Support for further analysis.

# What version of ElectricCommander does this tool support?

This tool is version-agnostic. As it talks directly to the database, it will work with any supported version of Commander.

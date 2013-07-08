package com.electric.cloud.plugins.hudson;
import hudson.Launcher;
import hudson.Extension;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import hudson.model.Cause;
import antlr.collections.List;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl\#newInstance(StaplerRequest)} is invoked
 * and a new {@link EctoolBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link \#ectool})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link \#perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked. 
 */
public class EctoolBuilder extends Builder {

    private final String ectool;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public EctoolBuilder(String ectool) {
        this.ectool = ectool;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getEctool() {
        return ectool;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // this is where you 'build' the project
    	listener.getLogger().println("Welcome to Electric Cloud's ectool plugin.");
    	
    	ArgumentListBuilder args = new ArgumentListBuilder();

		if (launcher.isUnix())
    	{
    		listener.getLogger().println("System is Linux");
    		// Note, this hasn't been tried on a Linux system with ectool installed...
    		args.add("/bin/ls");
    		args.add("-l");
    	}
    	else
    	{
    		listener.getLogger().println("System is Windows");
    		args.add("cmd");
    		args.add("/c");
    		args.add("ectool");
    	}

		// If the "ectool" is enclosed in quotes, we need to strip out the quote and then add each piece one by one.
		if (ectool.contains("\""))
		{
			ectool.replaceAll("\"", "");
		}
		
		if (ectool.contains(" "))
		{
			for (String p : ectool.split("\\s"))
			{
				args.add(p);
			}
		}
		else
		{
			args.add(ectool);
		}
		
		try {
			int r = 0;
			r = launcher.launch().cmds(args).stdout(listener).join();
			if (r != 0)
			{
				listener.finished(Result.FAILURE);
				return false;
			}
    	}
    	catch (IOException e) 
    	{
    		e.printStackTrace();
    		listener.getLogger().println("IOException " + args + "Failed.");
    		return false;
    	} catch (InterruptedException e) {
			e.printStackTrace();
    		listener.getLogger().println("InterruptException " + args + "Failed.");
			return false;
		}
		listener.finished(Result.SUCCESS);
        return true;
    }

    // overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link EctoolBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/ectool/EctoolBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;

        /**
         * Performs on-the-fly validation of the form field 'ectool'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckEctool(@QueryParameter String value) throws IOException, ServletException {
            if(value.length()==0)
                return FormValidation.error("Please enter your ectool parameters");
            if(value.length()<4)
                return FormValidation.warning("Isn't ectool too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "ElectricCloud ectool";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         */
        public boolean useFrench() {
            return useFrench;
        }
    }
}



// Configure.java --
//
// Configure.java is part of the PrivateDevelopmentCloud plugin.
//
// Copyright (c) 2005-2010 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.HomepagePlus.client.Configure;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.gwt.client.ComponentBase;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;
import com.electriccloud.commander.client.domain.Property;
import com.electriccloud.commander.client.requests.CommanderRequest;
import com.electriccloud.commander.client.requests.GetPropertyRequest;
import com.electriccloud.commander.client.requests.SetPropertyRequest;
import com.electriccloud.commander.client.responses.CommanderError;
import com.electriccloud.commander.client.responses.DefaultPropertyCallback;
import com.electriccloud.commander.client.responses.PropertyCallback;

/**
 * Basic component that is meant to be cloned and then customized to perform a
 * real function.
 */
public class Configure
    extends ComponentBase
{

    //~ Methods ----------------------------------------------------------------

    /**
     * This function is called by SDK infrastructure to initialize the UI parts of
     * this component.
     *
     * @return                 A widget that the infrastructure should place in
     *                         the UI; usually a panel.
     */
    @Override public Widget doInit()
    {

        // Simple caption-panel that declares the plugin name and
        // component name.
        CaptionPanel captionPanel = new CaptionPanel(ConfigureFactory
                    .getPluginName()
                + " Configure");

        final CheckBox cbAppServerDeployment = new CheckBox("Use AppServer Deployment flavour of demo");
        cbAppServerDeployment.setValue(false);

        final CheckBox cbRemoteAccess = new CheckBox("Use Remote Access");
        cbRemoteAccess.setValue(false);

        cbAppServerDeployment.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                updateProperty(ComponentBaseFactory.getPluginProjectPath()+"/useAppServerDeployment", cbAppServerDeployment.getValue().toString());
                cbRemoteAccess.setEnabled(!cbAppServerDeployment.getValue());
            }
        });
        cbRemoteAccess.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                updateProperty(ComponentBaseFactory.getPluginProjectPath()+"/useRemoteAccess", cbRemoteAccess.getValue().toString());
            }
        });

        VerticalPanel vp = new VerticalPanel();
        vp.add(cbAppServerDeployment);
        vp.add(cbRemoteAccess);
        captionPanel.add(vp);

        doRequest(new ChainedCallback() {

            @Override public void onComplete()
            {
                // no-op
            }
        },     setCheckBoxValue("useAppServerDeployment", cbAppServerDeployment),
            setCheckBoxValue("useRemoteAccess", cbRemoteAccess));

        
        // Fill in the caption panel...
        // We're done setting up the UI.  Return the panel.
        return captionPanel;
    }
    
    private GetPropertyRequest setCheckBoxValue(String aProperty, final CheckBox aCheckBox){
        GetPropertyRequest getPropertyRequest = getRequestFactory().createGetPropertyRequest();
        getPropertyRequest.setPropertyName(ComponentBaseFactory.getPluginProjectPath()+"/"+aProperty);
        getPropertyRequest.setCallback(new PropertyCallback() {
            @Override public void handleResponse(Property response)
            {
                String val = response.getValue();
                if(val.equals("1") || val.equals("true")){
                    aCheckBox.setValue(true);
                }
            }

            @Override public void handleError(CommanderError error)
            {
                // no-op, we don't care if this property doesn't exist
            }
        });
        return getPropertyRequest;
    }

    private void updateProperty(String aProperty, String aValue){
        SetPropertyRequest setPropertyRequest = getRequestFactory().createSetPropertyRequest();
        setPropertyRequest.setPropertyName(aProperty);
        setPropertyRequest.setValue(aValue);
        setPropertyRequest.setCallback(new DefaultPropertyCallback(this) {
            
            @Override public void handleResponse(Property response)
            {
                Location.reload();
            }
        });
        doRequest(setPropertyRequest);
    }
}

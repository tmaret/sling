/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.osgi.console.web.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * The <code>AjaxBundleDetailsAction</code> TODO
 * 
 * @scr.component metatype="false"
 * @scr.service
 */
public class AjaxBundleDetailsAction extends BundleAction {

    public static final String NAME = "ajaxBundleDetails";

    /** @scr.reference */
    private StartLevel startLevelService;

    /** @scr.reference */
    private PackageAdmin packageAdmin;

    // bootdelegation property entries. wildcards are converted to package
    // name prefixes. whether an entry is a wildcard or not is set as a flag
    // in the bootPkgWildcards array.
    // see #activate and #isBootDelegated
    private String[] bootPkgs;

    // a flag for each entry in bootPkgs indicating whether the respective
    // entry was declared as a wildcard or not
    // see #activate and #isBootDelegated
    private boolean[] bootPkgWildcards;

    public String getName() {
        return NAME;
    }

    public String getLabel() {
        return NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.manager.web.internal.Action#performAction(javax.servlet.http.HttpServletRequest)
     */
    public boolean performAction(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        JSONObject result = null;
        try {
            long bundleId = getBundleId(request);
            Bundle bundle = getBundleContext().getBundle(bundleId);
            if (bundle != null) {
                @SuppressWarnings("unchecked")
                Dictionary<String, String> headers = bundle.getHeaders();

                JSONArray props = new JSONArray();
                keyVal(props, "Symbolic Name", bundle.getSymbolicName());
                keyVal(props, "Version", headers.get(Constants.BUNDLE_VERSION));
                keyVal(props, "Location", bundle.getLocation());
                keyVal(props, "Last Modification", new Date(
                    bundle.getLastModified()));

                keyVal(props, "Vendor", headers.get(Constants.BUNDLE_VENDOR));
                keyVal(props, "Copyright",
                    headers.get(Constants.BUNDLE_COPYRIGHT));
                keyVal(props, "Description",
                    headers.get(Constants.BUNDLE_DESCRIPTION));

                keyVal(props, "Start Level", getStartLevel(bundle));

                listImportExport(props, bundle);

                listServices(props, bundle);

                result = new JSONObject();
                result.put(BundleListRender.BUNDLE_ID, bundleId);
                result.put("props", props);
            }
        } catch (Exception exception) {
            // create an empty result on problems
            result = new JSONObject();
        }

        // send the result
        response.setContentType("text/javascript");
        response.getWriter().print(result.toString());

        return false;
    }

    private Integer getStartLevel(Bundle bundle) {
        if (startLevelService == null) {
            return null;
        }

        return new Integer(startLevelService.getBundleStartLevel(bundle));
    }

    private void listImportExport(JSONArray props, Bundle bundle) {
        ExportedPackage[] exports = packageAdmin.getExportedPackages(bundle);
        if (exports != null && exports.length > 0) {
            // do alphabetical sort
            Arrays.sort(exports, new Comparator<ExportedPackage>() {
                public int compare(ExportedPackage p1, ExportedPackage p2) {
                    return p1.getName().compareTo(p2.getName());
                }
            });

            StringBuffer val = new StringBuffer();
            for (ExportedPackage export : exports) {

                boolean bootDel = isBootDelegated(export.getName());
                if (bootDel) {
                    val.append("<span style=\"color: red\">!! ");
                }

                val.append(export.getName());
                val.append(",version=");
                val.append(export.getVersion());

                if (bootDel) {
                    val.append(" -- Overwritten by Boot Delegation</span>");
                }

                val.append("<br />");
            }
            keyVal(props, "Exported Packages", val.toString());
        } else {
            keyVal(props, "Exported Packages", "None");
        }

        exports = packageAdmin.getExportedPackages((Bundle) null);
        if (exports != null && exports.length > 0) {
            // collect import packages first
            final List<ExportedPackage> imports = new ArrayList<ExportedPackage>();
            for (int i = 0; i < exports.length; i++) {
                final ExportedPackage ep = exports[i];
                final Bundle[] importers = ep.getImportingBundles();
                for (int j = 0; importers != null && j < importers.length; j++) {
                    if (importers[j].getBundleId() == bundle.getBundleId()) {
                        imports.add(ep);

                        break;
                    }
                }
            }
            // now sort
            StringBuffer val = new StringBuffer();
            if (imports.size() > 0) {
                final ExportedPackage[] packages = imports.toArray(new ExportedPackage[imports.size()]);
                Arrays.sort(packages, new Comparator<ExportedPackage>() {
                    public int compare(ExportedPackage p1, ExportedPackage p2) {
                        return p1.getName().compareTo(p2.getName());
                    }
                });
                // and finally print out
                for (ExportedPackage ep : packages) {

                    boolean bootDel = isBootDelegated(ep.getName());
                    if (bootDel) {
                        val.append("<span style=\"color: red\">!! ");
                    }

                    val.append(ep.getName());
                    val.append(",version=").append(ep.getVersion());
                    val.append(" from ");

                    if (ep.getExportingBundle().getSymbolicName() != null) {
                        // list the bundle name if not null
                        val.append(ep.getExportingBundle().getSymbolicName());
                        val.append(" (").append(
                            ep.getExportingBundle().getBundleId());
                        val.append(")");
                    } else if (ep.getExportingBundle().getLocation() != null) {
                        // otherwise try the location
                        val.append(ep.getExportingBundle().getLocation());
                        val.append(" (").append(
                            ep.getExportingBundle().getBundleId());
                        val.append(")");
                    } else {
                        // fallback to just the bundle id
                        // only append the bundle
                        val.append(ep.getExportingBundle().getBundleId());
                    }

                    if (bootDel) {
                        val.append(" -- Overwritten by Boot Delegation</span>");
                    }

                    val.append("<br />");
                }
            } else {
                // add description if there are no imports
                val.append("None");
            }

            keyVal(props, "Imported Packages", val.toString());
        }
    }

    private void listServices(JSONArray props, Bundle bundle) {
        ServiceReference[] refs = bundle.getRegisteredServices();
        if (refs == null || refs.length == 0) {
            return;
        }

        for (int i = 0; i < refs.length; i++) {
            String key = "Service ID "
                + refs[i].getProperty(Constants.SERVICE_ID);

            StringBuffer val = new StringBuffer();

            appendProperty(val, refs[i], Constants.OBJECTCLASS, "Types");
            appendProperty(val, refs[i], "sling.context", "Sling Context");
            appendProperty(val, refs[i], Constants.SERVICE_PID, "PID");
            appendProperty(val, refs[i], ConfigurationAdmin.SERVICE_FACTORYPID,
                "Factory PID");
            appendProperty(val, refs[i], ComponentConstants.COMPONENT_NAME,
                "Component Name");
            appendProperty(val, refs[i], ComponentConstants.COMPONENT_ID,
                "Component ID");
            appendProperty(val, refs[i], ComponentConstants.COMPONENT_FACTORY,
                "Component Factory");
            appendProperty(val, refs[i], Constants.SERVICE_DESCRIPTION,
                "Description");
            appendProperty(val, refs[i], Constants.SERVICE_VENDOR, "Vendor");

            keyVal(props, key, val.toString());
        }
    }

    private void appendProperty(StringBuffer dest, ServiceReference ref,
            String name, String label) {
        Object value = ref.getProperty(name);
        if (value instanceof Object[]) {
            Object[] values = (Object[]) value;
            dest.append(label).append(": ");
            for (int j = 0; j < values.length; j++) {
                if (j > 0) dest.append(", ");
                dest.append(values[j]);
            }
            dest.append("<br />"); // assume HTML use of result
        } else if (value != null) {
            dest.append(label).append(": ").append(value).append("<br />");
        }
    }

    private void keyVal(JSONArray props, String key, Object value) {
        if (key != null && value != null) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("key", key);
                obj.put("value", value);
                props.put(obj);
            } catch (JSONException je) {
                // don't care
            }
        }
    }

    // returns true if the package is listed in the bootdelegation property
    private boolean isBootDelegated(String pkgName) {

        // bootdelegation analysis from Apache Felix R4SearchPolicyCore

        // Only consider delegation if we have a package name, since
        // we don't want to promote the default package. The spec does
        // not take a stand on this issue.
        if (pkgName.length() > 0) {

            // Delegate any packages listed in the boot delegation
            // property to the parent class loader.
            for (int i = 0; i < bootPkgs.length; i++) {

                // A wildcarded boot delegation package will be in the form of
                // "foo.", so if the package is wildcarded do a startsWith() or
                // a regionMatches() to ignore the trailing "." to determine if
                // the request should be delegated to the parent class loader.
                // If the package is not wildcarded, then simply do an equals()
                // test to see if the request should be delegated to the parent
                // class loader.
                if ((bootPkgWildcards[i] && (pkgName.startsWith(bootPkgs[i]) || bootPkgs[i].regionMatches(
                    0, pkgName, 0, pkgName.length())))
                    || (!bootPkgWildcards[i] && bootPkgs[i].equals(pkgName))) {
                    return true;
                }
            }
        }

        return false;
    }

    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext context) {

        super.activate(context);

        // bootdelegation property parsing from Apache Felix R4SearchPolicyCore
        String bootDelegation = context.getBundleContext().getProperty(
            Constants.FRAMEWORK_BOOTDELEGATION);
        bootDelegation = (bootDelegation == null) ? "java.*" : bootDelegation
            + ",java.*";
        StringTokenizer st = new StringTokenizer(bootDelegation, " ,");
        bootPkgs = new String[st.countTokens()];
        bootPkgWildcards = new boolean[bootPkgs.length];
        for (int i = 0; i < bootPkgs.length; i++) {
            bootDelegation = st.nextToken();
            if (bootDelegation.endsWith("*")) {
                bootPkgWildcards[i] = true;
                bootDelegation = bootDelegation.substring(0,
                    bootDelegation.length() - 1);
            }
            bootPkgs[i] = bootDelegation;
        }

    }
}

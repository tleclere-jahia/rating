/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.rating.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author rincevent
 */
@Component(service = Action.class)
public class RateContent extends Action {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(RateContent.class);

    private JCRTemplate jcrTemplate;

    public RateContent() {
        setName("rate");
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, final Resource resource,
                                  JCRSessionWrapper session, final Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        return (ActionResult) jcrTemplate.doExecuteWithSystemSessionAsUser(null, session.getWorkspace().getName(), session.getLocale(), (JCRCallback<Object>) systemSession -> {
            JCRNodeWrapper node = systemSession.getNodeByUUID(resource.getNode().getIdentifier());
            if (!node.isNodeType("jmix:rating")) {
                systemSession.checkout(node);
                node.addMixin("jmix:rating");
                systemSession.save();
            }

            List<String> values = parameters.get("j:lastVote");
            node.setProperty("j:lastVote", values.get(0));
            node.setProperty("j:nbOfVotes", node.getProperty("j:nbOfVotes").getLong() + 1);
            node.setProperty("j:sumOfVotes", node.getProperty("j:sumOfVotes").getLong() + Long.parseLong(values.get(0)));
            node.setProperty("j:topRatedRatio", node.getProperty("j:sumOfVotes").getDouble()/node.getProperty("j:nbOfVotes").getDouble());

            systemSession.save();
            try {
                return new ActionResult(HttpServletResponse.SC_OK, node.getPath(), Render.serializeNodeToJSON(node));
            } catch (IOException | JSONException e) {
                logger.error("Error while creating rating action result", e);
            }
            return null;
        });
    }

    @Reference
    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }
}

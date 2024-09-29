package eu.doppelhelix.netbeans.plugin.rescan.rescan;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.DefaultCacheFolderProvider;
import org.netbeans.modules.parsing.impl.indexing.implspi.CacheFolderProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

@NbBundle.Messages({
    "CTL_RescanActionDescr=Rescan project"
})
@ActionID(
        category = "Project", //NOI18N
        id = "eu.doppelhelix.netbeans.plugin.rescan.rescan.RescanAction" //NOI18N
)
@ActionRegistration(
        displayName = "#CTL_RescanActionDescr",//NOI18N
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Projects/Actions/Test", position = 2000),
})
public class RescanAction extends NodeAction {

    private static final Logger LOG = Logger.getLogger(RescanAction.class.getName());

    private RescanAction() {
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        try {
            Lookup lookup = activatedNodes[0].getLookup();
            Project project = lookup.lookup(Project.class);
            List<URL> roots = new ArrayList<>();
            Set<FileObject> cacheFolders = new HashSet<>();
            for (URL rootUrl : DefaultCacheFolderProvider.getRootsInFolder(project.getProjectDirectory().toURL())) {
                try {
                    cacheFolders.add(CacheFolder.getDataFolder(rootUrl, true));
                } catch (IOException ex) {
                    LOG.log(Level.INFO, "Failed to find cache folder", ex);
                }
                for (CacheFolderProvider.Kind k : CacheFolderProvider.Kind.values()) {
                    try {
                        cacheFolders.add(CacheFolder.getDataFolder(rootUrl, EnumSet.of(k), CacheFolderProvider.Mode.EXISTENT));
                    } catch (IOException ex) {
                        LOG.log(Level.INFO, "Failed to find cache folder", ex);
                    }
                }
                roots.add(rootUrl);
            }
            for (FileObject fo : cacheFolders) {
                if (fo != null && fo.isValid()) {
                    try {
                        LOG.log(Level.INFO, "Removing cache folder: " + fo.getPath());
                        fo.delete();
                    } catch (IOException ex) {
                        LOG.log(Level.INFO, "Failed to delete cache folder: " + fo.getPath(), ex);
                    }
                }
            }
            for (URL rootUrl: roots) {
                LOG.log(Level.INFO, "Rrebuilding index for: " + rootUrl);
                IndexingManager.getDefault().refreshIndex(rootUrl, null, true);
            }
        } catch (IOException ex) {
            LOG.log(Level.INFO, "Failed to find cache folder", ex);
        }
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        if (activatedNodes.length == 1) {
            Lookup lookup = activatedNodes[0].getLookup();
            if (lookup.lookup(Project.class) != null) {
                return true;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return Bundle.CTL_RescanActionDescr();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

}

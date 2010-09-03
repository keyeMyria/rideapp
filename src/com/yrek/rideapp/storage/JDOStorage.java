package com.yrek.rideapp.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.Query;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Blob;

public class JDOStorage implements Storage {
    private static final Logger LOG = Logger.getLogger(JDOStorage.class.getName());

    private final PersistenceManagerFactory persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory("transactions-optional");

    @PersistenceCapable
    static class File {
        @PrimaryKey @Persistent String path;
        @Persistent String dir;
        @Persistent Blob content;
    }

    public String[] listFiles(String dir) {
        PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
        try {
            Query query = persistenceManager.newQuery(File.class);
            try {
                query.setFilter("dir == listDir");
                query.setOrdering("path asc");
                query.declareParameters("String listDir");
                ArrayList<String> files = new ArrayList<String>();
                for (Object o : (List) query.execute(dir))
                    files.add(((File) o).path.substring(dir.length()));
                return files.toArray(new String[files.size()]);
            } finally {
                query.closeAll();
            }
        } finally {
            persistenceManager.close();
        }
    }

    public byte[] readFile(String path) {
        PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
        try {
            return persistenceManager.getObjectById(File.class, path).content.getBytes();
        } catch (JDOObjectNotFoundException e) {
            return null;
        } finally {
            persistenceManager.close();
        }

    }

    public void deleteFile(String path) {
        PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
        try {
            persistenceManager.deletePersistent(persistenceManager.getObjectById(File.class, path));
        } finally {
            persistenceManager.close();
        }
    }

    public void writeFile(String path, byte[] content) {
        File file = new File();
        file.path = path;
        file.dir = getDir(path);
        file.content = new Blob(content);
        PersistenceManager persistenceManager = persistenceManagerFactory.getPersistenceManager();
        try {
            persistenceManager.makePersistent(file);
        } finally {
            persistenceManager.close();
        }
    }

    private String getDir(String path) {
        return path.substring(0, path.lastIndexOf('/')+1);
    }
}

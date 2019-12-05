package edu.gmu.cs475;

import edu.gmu.cs475.struct.ITag;
import edu.gmu.cs475.struct.ITaggedFile;
import edu.gmu.cs475.struct.NoSuchTagException;
import edu.gmu.cs475.struct.TagExistsException;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.StampedLock;

public class FileTagManager extends AbstractFileTagManager {
    //used to keep track of the tags in use
    //There may be issues with using this in concurrent cases, replacing it with a set obtained from ConcurrentHashMap may help with that
    private HashSet<Tag> tagList = new HashSet<>();
    //    private Set<Tag> tagList = Collections.synchronizedSet(new HashSet<>());
    private StampedLock lock = new StampedLock();
//    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
//    private long lockCounter = 0;

    @Override
    public Iterable<? extends ITag> listTags() {

//        /*
        HashSet<Tag> tags = new HashSet<>(tagList.size());
        synchronized (this) {
            for (Tag tag : tagList) {
                tags.add(tag);
            }
        }
        return tags;

//        return tagList;
    }

    @Override
    public ITag addTag(String name) throws TagExistsException {
        //TODO: add concurrent stuff
        Tag t = new Tag(name);

//        ReentrantLock rl = new ReentrantLock();
//        rl.lock();
//        synchronized (tagList) {
        synchronized (tagList) {
            if (!tagList.add(t)) {
                throw new TagExistsException();
            }
        }
//        }
//        rl.unlock();
        return t;
    }

    @Override
    public ITag editTag(String oldTagName, String newTagName) throws TagExistsException, NoSuchTagException {
        //TODO: add concurrent stuff
        for (Tag t : tagList) {
            if (t.getName().equals(newTagName)) {
                throw new TagExistsException();
            }
        }
        for (Tag t : tagList) {
            if (t.getName().equals(oldTagName)) {
                synchronized (this) {
                    t.setName(newTagName);
                    return t;
                }
            }
        }
        throw new NoSuchTagException();
    }

    @Override
    public ITag deleteTag(String tagName) throws NoSuchTagException, DirectoryNotEmptyException {
        //TODO: add concurrent stuff
        synchronized (this) {
            for (Tag t : (Iterable<Tag>) listTags()) {

                if (t.getName().equals(tagName)) {
                    if (!t.files.isEmpty()) {
                        throw new DirectoryNotEmptyException(tagName);
                    }
//                synchronized (this) {
                    if (!tagList.remove(t)) {
                        throw new NoSuchTagException();
                    }
                    return t;
//                }
                }
            }
        }
        throw new NoSuchTagException();
    }

    @Override
    public void init(List<Path> files) {
        //
        try {
            this.addTag("untagged");
        } catch (TagExistsException e) {
            System.err.println("Tag already exists when no tags should exist");
        }
        for (Tag tag : tagList) {
            for (Path p : files) {
                TaggedFile file = new TaggedFile(p);
                file.tags.add(tag);
                tag.files.add(file);
            }
        }

    }

    @Override
    public Iterable<? extends ITaggedFile> listAllFiles() {
        // TODO concurrent stuff
        HashSet<TaggedFile> files = new HashSet<>();
        synchronized (this) {
            for (Tag tag : tagList) {
                for (TaggedFile file : tag.files) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    @Override
    public Iterable<? extends ITaggedFile> listFilesByTag(String tag) throws NoSuchTagException {
        // TODO concurrent stuff
        for (ITag t : listTags()) {
            if (t.getName().equals(tag)) {
                HashSet<TaggedFile> files = new HashSet<>();
                synchronized (t) {
                    for (TaggedFile file : ((Tag) t).files) {
                        files.add(file);
                    }
                }
                return files;
            }
        }
        throw new NoSuchTagException();
    }

    @Override
    public boolean tagFile(String file, String tag) throws NoSuchFileException, NoSuchTagException {
        // TODO concurrent stuff
        Tag t = null;
        for (ITag x : listTags()) {
            if (x.getName().equals(tag)) {
                t = (Tag) x;
            }
        }
        if (t == null) {
            throw new NoSuchTagException();
        }

        for (TaggedFile taggedFile : (Iterable<TaggedFile>) listAllFiles()) {
            synchronized (this) {
                if (taggedFile.getName().equals(file)) {
                    if (taggedFile.tags.contains(new Tag("untagged"))) {
                        taggedFile.tags.remove(new Tag("untagged"));
                    } else if (taggedFile.tags.contains(t)) {
                        return false;
                    }
                    if(!((HashSet<Tag>) listTags()).contains(t)){
                        throw new NoSuchTagException();
                    }
                    taggedFile.tags.add(t);
                    t.files.add(taggedFile);
                    return true;
                }
            }
        }
        throw new NoSuchFileException(file);
    }

    @Override
    public boolean removeTag(String file, String tag) throws NoSuchFileException, NoSuchTagException {
        // TODO concurrent stuff
        if (tag.equals("untagged")) {
            return false;
        }
        Tag t = null;
        for (Tag x : tagList) {
            if (x.getName().equals(tag)) {
                t = x;
            }
        }
        if (t == null) {
            throw new NoSuchTagException();
        }
        for (TaggedFile taggedFile : (Iterable<TaggedFile>) listAllFiles()) {
            synchronized (this) {
                if (taggedFile.getName().equals(file)) {
                    if (!taggedFile.tags.contains(t)) {
                        return false;
                    }

                    taggedFile.tags.remove(t);
                    t.files.remove(taggedFile);

                    if (taggedFile.tags.size() == 0) {
                        taggedFile.tags.add(new Tag("untagged"));
                    }
                    return true;
                }
            }
        }
        throw new NoSuchFileException(file);
    }

    @Override
    public Iterable<? extends ITag> getTags(String file) throws NoSuchFileException {
        // TODO concurrent stuff
        for (TaggedFile taggedFile : (Iterable<TaggedFile>) listAllFiles()) {
            synchronized (this) {
                if (taggedFile.getName().equals(file)) {
                    return taggedFile.tags;
                }
            }
        }
        throw new NoSuchFileException(file);
    }

    @Override
    public String catAllFiles(String tag) throws NoSuchTagException, IOException {
        // TODO concurrent stuff
        if (!tagList.contains(new Tag(tag))) {
            throw new NoSuchTagException();
        }
        Iterable<TaggedFile> files = (Iterable<TaggedFile>) listFilesByTag(tag);
        StringBuilder str = new StringBuilder();
//        synchronized (this) {
        for (TaggedFile taggedFile : files) {
            long stamp = lockFile(taggedFile.getName(), false);
            str.append(readFile(taggedFile.getName()));
            unLockFile(taggedFile.getName(), stamp, false);
        }
//        }
        return str.toString();
    }

    @Override
    public void echoToAllFiles(String tag, String content) throws NoSuchTagException, IOException {
        // TODO Auto-generated method stub
        if (!tagList.contains(new Tag(tag))) {
            throw new NoSuchTagException();
        }
        Iterable<TaggedFile> files = (Iterable<TaggedFile>) listFilesByTag(tag);
//        synchronized (this) {
        for (TaggedFile taggedFile : files) {
            long stamp = lockFile(taggedFile.getName(), true);
            writeFile(taggedFile.getName(), content);
            unLockFile(taggedFile.getName(), stamp, true);
        }
//        }
    }

    @Override
    public long lockFile(String name, boolean forWrite) throws NoSuchFileException {
//        StampedLock lock = new StampedLock();
//        ReadWriteLock view = lock.asReadWriteLock();
        for (ITaggedFile file : listAllFiles()) {
            if (file.getName().equals(name)) {
                if (forWrite) {
//                    lockCounter ++;
//                    lock.writeLock();
//                    lo
                    return lock.tryWriteLock();
                } else {
                    return lock.tryReadLock();
                }
            }
        }
        throw new NoSuchFileException(name);
    }

    @Override
    public void unLockFile(String name, long stamp, boolean forWrite) throws NoSuchFileException {
//        StampedLock lock = new StampedLock();
        for (ITaggedFile file : listAllFiles()) {
            if (file.getName().equals(name)) {
                if (forWrite) {
                    lock.tryUnlockWrite();
                    return;
                } else {
                    lock.tryUnlockRead();
                    return;
                }
            }
        }
        throw new NoSuchFileException(name);
    }
}
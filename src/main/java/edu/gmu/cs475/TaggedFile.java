package edu.gmu.cs475;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.locks.StampedLock;

import edu.gmu.cs475.struct.ITaggedFile;

public class TaggedFile implements ITaggedFile {
	public HashSet<Tag> tags = new HashSet<>();
	public StampedLock lock = new StampedLock();
	
	public StampedLock getLock() {
		return lock;
	}
	private Path path;
	public TaggedFile(Path path)
	{
		this.path = path;
	}
	@Override
	public String getName() {
		return path.toString();
	}
	@Override
	public String toString() {
		return getName();
	}
    @Override
    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(!(o instanceof TaggedFile)){
            return false;
        }
        TaggedFile obj = (TaggedFile) o;
        return this.getName().equals(obj.getName());
    }
	@Override
	public int hashCode(){
		return this.getName().hashCode();
	}
}

package edu.gmu.cs475;

import edu.gmu.cs475.internal.Command;
import edu.gmu.cs475.internal.DeadlockDetectorAndRerunRule;
import edu.gmu.cs475.struct.ITag;
import edu.gmu.cs475.struct.ITaggedFile;
import edu.gmu.cs475.struct.NoSuchTagException;
import edu.gmu.cs475.struct.TagExistsException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NoSuchFileException;
import java.util.*;

public class ConcurrentTests {
	/* Leave at 6 please */
	public static final int N_THREADS = 6;

	@Rule
	public DeadlockDetectorAndRerunRule timeout = new DeadlockDetectorAndRerunRule(10000);

	/**
	 * Use this instance of fileManager in each of your tests - it will be
	 * created fresh for each test.
	 */
	AbstractFileTagManager fileManager;

	/**
	 * Automatically called before each test, initializes the fileManager
	 * instance
	 */
	@Before
	public void setup() throws IOException {
		fileManager = new FileTagManager();
		fileManager.init(Command.listAllFiles());
	}

	/**
	 * Create N_THREADS threads, with half of the threads adding new tags and
	 * half iterating over the list of tags and counting the total number of
	 * tags. Each thread should do its work (additions or iterations) 1,000
	 * times. Assert that the additions all succeed and that the counting
	 * doesn't throw any ConcurrentModificationException. There is no need to
	 * make any assertions on the number of tags in each step.
	 */
	@Test
	public void testP1AddAndListTag() {

		ArrayList<Thread> threads = new ArrayList<>(N_THREADS);
		for (int j = 0; j < N_THREADS / 2; j++) {
			final int g = j;
			threads.add(new Thread(new Runnable() {

				private int threadNum = g;

				@Override
				public void run() {
					for (int i = 0; i < 1000; i++) {
						try {
							fileManager.addTag(Integer.toString(threadNum * 1000 + i));
						} catch (TagExistsException e) {
							Assert.assertTrue("Adding tag already in fileManager, should not be possible", false);
						} catch (ConcurrentModificationException e) {
							Assert.assertTrue("concurrent modification occurred", false);
						}
					}

				}
			}));
		}
		for (int j = N_THREADS / 2; j < N_THREADS; j++) {
			final int g = j;
			threads.add(new Thread(new Runnable() {
				private final int threadNum = g;

				@Override
				public void run() {
//             Iterable<? extends ITag> tags = fileManager.listTags();
					int count = 0;
					for (ITag tag : fileManager.listTags()) {
						count++;
					}
				}
			}));
		}

		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		int numTags = 0;
		for (ITag tag : fileManager.listTags()) {
			numTags++;
		}
		Assert.assertEquals((N_THREADS / 2) * 1000 + 1, numTags);
	}

	/**
	 * Create N_THREADS threads, and have each thread add 1,000 different tags;
	 * assert that each thread creating a different tag succeeds, and that at
	 * the end, the list of tags contains all of tags that should exist
	 */
	@Test
	public void testP1ConcurrentAddTagDifferentTags() {
		ArrayList<Thread> threads = new ArrayList<>(N_THREADS);
		for (int j = 0; j < N_THREADS; j++) {
			final int g = j;
			threads.add(new Thread(new Runnable() {
				final int threadNum = g;

				@Override
				public void run() {
					for (int i = 0; i < 1000; i++) {
						try {
							ITag t = fileManager.addTag(Integer.toString(threadNum * 1000 + i));
							Assert.assertNotEquals(t, null);
						} catch (TagExistsException e) {
							Assert.assertTrue("tag already exists in fileTagManager, should not be possible", false);
						}
					}
				}
			}));

		}
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		HashSet<String> set = new HashSet<>();
		set.add("untagged");
		for (int i = 0; i < N_THREADS * 1000; i++) {
			set.add(Integer.toString(i));
		}

		for (ITag tag : fileManager.listTags()) {
			Assert.assertTrue(set.contains(tag.getName()));
		}
     /*
     for(int i = 0;i < N_THREADS * 1000; i++){
        boolean found = false;
        for(ITag tag: fileManager.listTags()){
           if(tag.getName().equals(Integer.toString(i))){
              found = true;
              break;
           }
        }
        Assert.assertTrue("fuck", found);
     }
     */


	}

	/**
	 * Create N_THREADS threads. Each thread should try to add the same 1,000
	 * tags of your choice. Assert that each unique tag is added exactly once
	 * (there will be N_THREADS attempts to add each tag). At the end, assert
	 * that all tags that you created exist by iterating over all tags returned
	 * by listTags()
	 */
	@Test
	public void testP1ConcurrentAddTagSameTags() {
		ArrayList<Thread> threads = new ArrayList<>();
		for (int i = 0; i < N_THREADS; i++) {
			threads.add(new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 1000; i++) {
						try {
							fileManager.addTag(Integer.toString(i));
						} catch (TagExistsException e) {
//                   e.printStackTrace();
//                   Assert.assertTrue(false);
						}
					}
				}
			}));
		}

		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		HashSet<Tag> set = new HashSet();
		set.add(new Tag("untagged"));
		for (int i = 0; i < 1000; i++) {
			set.add(new Tag(Integer.toString(i)));
		}
		for (ITag tag : fileManager.listTags()) {
			Assert.assertTrue(set.contains(tag));
		}

	}

	/**
	 * Create 1000 tags. Save the number of files (returned by listFiles()) to a
	 * local variable.
	 * <p>
	 * Then create N_THREADS threads. Each thread should iterate over all files
	 * (from listFiles()). For each file, it should select a tag and random from
	 * the list returned by listTags(). Then, it should tag that file with that
	 * tag. Then (regardless of the tagging succeeding or not), it should pick
	 * another random tag, and delete it. You do not need to care if the
	 * deletions pass or not either.
	 * <p>
	 * <p>
	 * At the end (once all threads are completed) you should check that the
	 * total number of files reported by listFiles matches what it was at the
	 * beginning. Then, you should list all of the tags, and all of the files
	 * that have each tag, making sure that the total number of files reported
	 * this way also matches the starting count. Finally, check that the total
	 * number of tags on all of those files matches the count returned by
	 * listTags.
	 */
	@Test
	public void testP2ConcurrentDeleteTagTagFile() throws Exception {
		ArrayList<Thread> threads = new ArrayList<>(N_THREADS);
		Random r = new Random();
		for (int i = 0; i < 1000; i++) {
			Tag t = new Tag(Integer.toString(i));
			fileManager.addTag(t.getName());
		}

		int numFiles = 0;
		for (TaggedFile f : (Iterable<TaggedFile>) fileManager.listAllFiles()) {
			numFiles += 1;
		}

		for (int j = 0; j < N_THREADS; j++) {
			final int g = j;
			threads.add(new Thread(new Runnable() {
				final int threadNum = g;

				@Override
				public void run() {
					for (TaggedFile f : (Iterable<TaggedFile>) fileManager.listAllFiles()) {
						try {
							ITag tag1 = null;
							ITag tag2 = null;
							synchronized (fileManager){
							int rando = r.nextInt(((HashSet<ITag>) fileManager.listTags()).size());
							int rando2 = r.nextInt(((HashSet<ITag>) fileManager.listTags()).size());
							int counter = 0;
							int hit = 0;
							for(ITag t : fileManager.listTags()){
								if(counter == rando){
									tag1 = t;
									hit +=1;
								}
								if(counter == rando2){
									tag2 = t;
									hit +=1;
								}
								if(hit == 2){
									break;
								}
								counter +=1;
							}
							}
							//Assert.assertNotEquals(tag1, null);
							fileManager.tagFile(f.getName(), tag1.getName());
							//Assert.assertNotEquals(tag2, null);
							fileManager.deleteTag(tag2.getName());
						} catch (NoSuchTagException e) {
							//Assert.assertTrue(false);
						} catch (NoSuchFileException e) {
							Assert.assertTrue("No file by this name found", false);
						}
						catch(DirectoryNotEmptyException e){
							//Assert.assertTrue(false);
						}
					}
				}
			}));
		}

		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		int count = 0;
		HashSet<Tag> tagHashSet = new HashSet<>();
		tagHashSet.add(new Tag("untagged"));
		for (TaggedFile f : (Iterable<TaggedFile>) fileManager.listAllFiles()) {
			count += 1;
			for (ITag tag : fileManager.getTags(f.getName())) {
				tagHashSet.add((Tag) tag);
			}
		}
		Assert.assertEquals(numFiles, count);
		int tagCount = 0;
		HashSet<ITaggedFile> fileHashSet = new HashSet<>(numFiles);
		for (ITag t : fileManager.listTags()) {
			tagCount += 1;
			for (ITaggedFile f : fileManager.listFilesByTag(t.getName())) {
				fileHashSet.add(f);
			}
		}
		Assert.assertEquals(numFiles, fileHashSet.size());
		//Assert.assertEquals((HashSet<Tag>) fileManager.listTags(), tagHashSet);
		Assert.assertEquals(tagCount, tagHashSet.size());
	}

  /*
  @Test
  public void testP2ConcurrentDeleteTagTagFile() throws Exception {
     //create 1000 tags
     for(int i=0; i<1000; i++){
        fileManager.addTag(Integer.toString(i));
     }
     //save number of files
     int numFiles = 0;
     for(ITaggedFile file: fileManager.listAllFiles()){
        numFiles ++;
     }

     //create threads
     ArrayList<Thread> threads = new ArrayList<>();
     for(int i=0; i<N_THREADS; i++){
        final int g = i;

        threads.add(new Thread(new Runnable() {
           int threadNum = g;

           @Override
           public void run() {
              Random rng = new Random();
              HashSet<TaggedFile> fileSet = (HashSet<TaggedFile>) fileManager.listAllFiles();
              //iterate over all files
              for (ITaggedFile file : fileSet) {
                 HashSet<Tag> tagSet = (HashSet<Tag>) fileManager.listTags();
                 //select tag at random
//                int addNum = rng.nextInt() % tagSet.size();
                 int addNum = rng.nextInt(tagSet.size());
                 //select a tag at random from the list of tags
                 int cur = 0;
                 for(ITag tag: tagSet){
                    if(cur == addNum){
                       //tag the file with that tag (does not need to check for success)
                       try {
                          fileManager.tagFile(file.getName(), tag.getName());
                       }
                       catch (NoSuchTagException e){
//                         Assert.assertFalse("no such tag", true);
                       }
                       catch (NoSuchFileException e){
                          Assert.assertFalse("no such file", true);
                       }
                    }
                    cur += 1;
                 }
                 //select another tag at random
                 int delNum = rng.nextInt() % tagSet.size();
                 cur = 0;
                 for(ITag tag: fileManager.listTags()){
                    if(cur == delNum){
                       //tag the file with that tag (does not need to check for success)
                       try {
                          //delete that tag (does not need to check for success)
                          fileManager.deleteTag(tag.getName());
                       }
                       catch (NoSuchTagException e){
//                         Assert.assertFalse("no such tag", true);
                       }
                       catch (DirectoryNotEmptyException e){
//                         Assert.assertFalse("files are still tagged with the tag", true);
                       }
                    }
                    cur += 1;
                 }
              }
           }
        }));
     }

     for(Thread thread: threads){
        thread.start();
     }
     for(Thread thread: threads){
        try {
           thread.join();
        } catch (InterruptedException e) {
           e.printStackTrace();
        }
     }


     //check that the total number of files retrieved from listFiles is unchanged
     int fileCount = 0;
     for(ITaggedFile file: fileManager.listAllFiles()){
        fileCount += 1;
     }

     Assert.assertEquals(numFiles, fileCount);

     //list all tags, and all files that have each tag
//    int numFilesByTag = 0;
     HashSet<String> tagHashSet = new HashSet<>();
     for(ITag tag: fileManager.listTags()){
        for(ITaggedFile file: fileManager.listFilesByTag(tag.getName())){
//          numFilesByTag += 1;
           tagHashSet.add(tag.getName());
        }
     }
     Assert.assertEquals(numFiles, tagHashSet.size());
  }
  */

	/**
	 * Create a tag. Add each tag to every file. Then, create N_THREADS and have
	 * each thread iterate over all of the files returned by listFiles(),
	 * calling removeTag on each to remove that newly created tag from each.
	 * Assert that each removeTag succeeds exactly once.
	 *
	 * @throws Exception
	 */
	@Test
	public void testP2RemoveTagWhileRemovingTags() throws Exception {
     /*
     //create a tag
     ITag tag = fileManager.addTag("testTag");
     //add each tag to every file
     for(ITaggedFile file: fileManager.listAllFiles()){
        fileManager.tagFile(file.getName(), tag.getName());
     }
     */
		//create N_THREADS
		//have each thread iterate over all the files
		//call removeTag on each file to remove the tag
		//Assert that each removeTag succeeds exactly once

		ITag t = fileManager.addTag("new tag");
		HashMap<ITaggedFile, Integer> map = new HashMap<>();
		int numFiles = 0;
		for (TaggedFile f : (Iterable<TaggedFile>) fileManager.listAllFiles()) {
			fileManager.tagFile(f.getName(), t.getName());
			map.put(f, 0);
			numFiles += 1;
		}

//        ArrayList<Integer> ary = new ArrayList<Integer>();
//        HashMap<ITaggedFile, Integer> dict = new HashMap<>();


		ArrayList<Thread> threads = new ArrayList<>(N_THREADS);
		for (int j = 0; j < N_THREADS; j++) {
			final int g = j;
			threads.add(new Thread(new Runnable() {

				@Override
				public void run() {
					for (TaggedFile f : (Iterable<TaggedFile>) fileManager.listAllFiles()) {
						try {
							boolean removed = fileManager.removeTag(f.getName(), t.getName());
							synchronized (map) {
								if (!removed) {
									map.put(f, map.get(f) + 1);
								}
							}

						} catch (NoSuchTagException e) {
//                            map.put(f, map.get(f) + 1);
						} catch (NoSuchFileException e) {
							Assert.assertTrue("No file by this name found", false);
						}
					}
				}
			}));
		}

		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		int count = 0;
       /*
       for (int k = 0; k < ary.size(); k++) {
           if (ary.get(k) == 1) {
               count += 1;
           }
       }
       */
		for(Integer numFailures: map.values()){
			Assert.assertEquals(Integer.valueOf(N_THREADS - 1), numFailures);
		}
//        Assert.assertEquals(((N_THREADS - 1) * numFiles), count);


	}

	/**
	 * Create N_THREADS threads and N_THREADS/2 tags. Half of the threads will
	 * attempt to tag every file with (a different) tag. The other half of the
	 * threads will count the number of files currently having each of those
	 * N_THREADS/2 tags. Assert that there all operations succeed, and that
	 * there are no ConcurrentModificationExceptions. Do not worry about how
	 * many files there are of each tag at each step (no need to assert on
	 * this).
	 */
	@Test
	public void testP2TagFileAndListFiles() throws Exception {
		ArrayList<ITag> tags = new ArrayList<>();
		for (int i = 0; i < N_THREADS / 2; i++) {
			tags.add(fileManager.addTag(Integer.toString(i)));
		}
		//create N_threads threads
		ArrayList<Thread> threads = new ArrayList<>();
		//create N_THREADS tags
		for (int i = 0; i < N_THREADS / 2; i++) {
			final int g = i;
			threads.add(new Thread(new Runnable() {
				int threadNum = g;

				@Override
				public void run() {
//             fileManager.addTag()
					//tag every file with associated tag
					for (ITaggedFile file : fileManager.listAllFiles()) {
						try {
							fileManager.tagFile(file.getName(), Integer.toString(threadNum));
						} catch (NoSuchFileException e) {
							e.printStackTrace();
						} catch (NoSuchTagException e) {
							e.printStackTrace();
						}
					}
				}
			}));
		}
		//count the number of files currently having each of those tags
		for (int i = N_THREADS / 2; i < N_THREADS; i++) {
			final int g = i;
			threads.add(new Thread(new Runnable() {
				int threadNum = g;

				@Override
				public void run() {
					int count = 0;

					try {
						for (ITaggedFile file : fileManager.listFilesByTag(Integer.toString(threadNum - threadNum / 2))) {
							count++;
						}
					} catch (NoSuchTagException e) {
//                e.printStackTrace();
					} catch (ConcurrentModificationException e) {
						e.printStackTrace();
						Assert.assertFalse("Concurrent Modification Exception occurs", true);
					}
				}
			}));
		}

     /*
     for(int i=0; i<N_THREADS/2; i++){
        final int g = i;
        threads.add(new Thread(new Runnable() {
           int threadNum = g;
           @Override
           public void run() {
              try {
                 fileManager.addTag(Integer.toString(threadNum));
              }
              catch (TagExistsException e){
                 Assert.assertFalse("tag already exists", true);
              }
              for(ITaggedFile file: fileManager.listAllFiles()){
                 try {
                    fileManager.tagFile(file.getName(), Integer.toString(threadNum));
                 }
                 catch (NoSuchTagException e){
                    Assert.assertFalse("no such tag", true);
                 }
                 catch (NoSuchFileException e){
                    Assert.assertFalse("no such file", true);
                 }
              }
           }
        }));
     }
     for (int i=N_THREADS/2; i < N_THREADS; i++){
        final int g = i;
        threads.add(new Thread(new Runnable() {
           int threadNum = g;

           @Override
           public void run() {
               for (ITag tag: fileManager.listTags()){
                 int count = 0;
                 try {
                    for (ITaggedFile file : fileManager.listFilesByTag(tag.getName())) {
                       count += 1;
                    }
                 }
                 catch (NoSuchTagException e){
                    Assert.assertFalse("no such tag", true);
                 }
               }
           }
        }));
     }
     */
		for (Thread thread : threads) {
			thread.start();
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Create N_THREADS threads, and have each try to echo some text into all of
	 * the files using echoAll. At the end, assert that all files have the same
	 * text.
	 */
	@Test
	public void testP3ConcurrentEchoAll() throws Exception {
		ArrayList<Thread> threads = new ArrayList<>();
		for (int i = 0; i < N_THREADS; i++) {
			final int g = i;
			threads.add(new Thread(new Runnable() {
				int threadNum = g;

				@Override
				public void run() {
					try {
						fileManager.echoToAllFiles("untagged", "some text");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}));
		}
		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		for (ITaggedFile file : fileManager.listAllFiles()) {
//            System.out.println(fileManager.readFile(file.getName()));
			Assert.assertEquals("some text", fileManager.readFile(file.getName()));
		}
	}

	/**
	 * Create N_THREADS threads, and have half of those threads try to echo some
	 * text into all of the files. The other half should try to cat all of the
	 * files, asserting that all of the files should always have the same
	 * content.
	 */
	@Test
	public void testP3EchoAllAndCatAll() throws Exception {
       /*
       ArrayList<Thread> threads = new ArrayList<>();
       for (int i = 0; i < N_THREADS / 2; i++) {
           final int g = i;
           threads.add(new Thread(new Runnable() {
               int threadNum = g;

               @Override
               public void run() {
                   fileManager.echoToAllFiles();
               }
           }));
       }
       for (Thread thread : threads) {
           thread.start();
       }
       for (Thread thread : threads) {
           try {
               thread.join();
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
       */

		HashSet<String> strings = new HashSet<>();
		ArrayList<Thread> threads = new ArrayList<>();
		for (int i = 0; i < N_THREADS/2; i++) {
			final int g = i;
			threads.add(new Thread(new Runnable() {
				int threadNum = g;

				@Override
				public void run() {
					try {
						fileManager.echoToAllFiles("untagged", "placeholder");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}));
		}
		for(int i= N_THREADS/2; i<N_THREADS; i++){
			final int g = i;

			threads.add(new Thread(new Runnable() {
				int threadNum = g;

				@Override
				public void run() {
					try {
						String str = fileManager.catAllFiles("untagged");
						synchronized (this){
							strings.add(str);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}));
		}

		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Assert.assertEquals(1, strings.size());
	}
}
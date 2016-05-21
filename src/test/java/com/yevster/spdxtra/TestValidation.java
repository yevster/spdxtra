package com.yevster.spdxtra;

import org.junit.Test;

import com.yevster.spdxtra.model.Creator;

/*
 * A tiny sampling of various validations 
 */
public class TestValidation {
	
	@Test(expected=IllegalArgumentException.class)	
	public void illegalPackageSpdxId(){
		Write.Document.addDescribedPackage("http://legalbaseUrl:7100", "SPDXRef-1", "SPDXRiiiiiiight-2", "Package McPackageFace");
	}

	@Test(expected=IllegalArgumentException.class)	
	public void illegalFileSpdxId(){
		Write.Package.addFile("http://legalbaseUrl:7100", "SPDXRef-1", "SPDXRiiiiiiight-2", "Filey McFileFace");
	}
	
	@Test(expected=IllegalArgumentException.class)	
	public void illegalUriOnPropertySet(){
		Write.Package.homepage("http://fooseyMcBoosey#Whats#Up", NoneNoAssertionOrValue.NO_ASSERTION);
	}
	
	@Test(expected=IllegalArgumentException.class)	
	public void illegalDocumentSpdxIdOnCreation(){
		Write.New.document("http://legalbaseUrl:7100", "SPDXRefsky-1", "yo", Creator.tool("What a tool!"));
	}
	
	@Test(expected=IllegalArgumentException.class)	
	public void testIllegalBaseUrl(){
		Write.Document.creationComment("http://illegalBaseUrl#Orwhat", "SPDXRef-DockyTheDocument", "Yep!");
	}
	
	@Test(expected=IllegalArgumentException.class)	
	public void nullCopyrightValue(){
		Write.File.copyrightText("http://legaluri.com#SPDXRef-NiceFile", null);
	}
	
	@Test(expected=IllegalArgumentException.class)	
	public void nullStringPropertyValue(){
		Write.Package.summary("http://legaluri.com#SPDXRef-1", null);
	}
}

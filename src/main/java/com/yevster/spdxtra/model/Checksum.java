package com.yevster.spdxtra.model;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxResourceTypes;
import com.yevster.spdxtra.SpdxUris;

public final class Checksum {
	public static enum Algorithm {
		SHA1, SHA256, MD5;

		private static final String URI_PREPEND = SpdxUris.SPDX_TERMS + "checksumAlgorithm_";

		public String getUri() {
			return URI_PREPEND + StringUtils.lowerCase(this.name());
		}

		public static Algorithm fromUri(String uri) {
			String name = StringUtils.removeStart(uri, URI_PREPEND);
			return Algorithm.valueOf(StringUtils.upperCase(name));
		}
	}

	public Resource asResource(Model model) {
		Resource rdfResource = model.createResource();
		rdfResource.addProperty(SpdxProperties.RDF_TYPE, SpdxResourceTypes.CHECKSUM_TYPE);
		rdfResource.addProperty(SpdxProperties.CHECKSUM_VALUE, Objects.requireNonNull(digest));
		rdfResource.addProperty(SpdxProperties.CHECKSUM_ALGORITHM,
				ResourceFactory.createResource(Objects.requireNonNull(algorithm).getUri()));
		return rdfResource;
	}

	public static Checksum sha1(String digest) {
		return new Checksum(Algorithm.SHA1, digest);
	}

	public static Checksum sha256(String digest) {
		return new Checksum(Algorithm.SHA256, digest);
	}

	public static Checksum md5(String digest) {
		return new Checksum(Algorithm.MD5, digest);
	}

	private Algorithm algorithm;
	private String digest;

	private Checksum(Algorithm algorithm, String digest) {
		this.algorithm = algorithm;
		this.digest = digest;
	}

	public static Checksum fromResource(Resource rdfResource) {
		Algorithm algorithm = Algorithm.fromUri(rdfResource.getPropertyResourceValue(SpdxProperties.CHECKSUM_ALGORITHM).getURI());
		String digest = rdfResource.getProperty(SpdxProperties.CHECKSUM_VALUE).getLiteral().getString();
		return new Checksum(algorithm, digest);
	}

	/**
	 * Returns the algorithm for this checksum.
	 * 
	 * @return
	 */
	public Algorithm getAlgorithm() {
		return algorithm;
	}

	/**
	 * Returns the digest portion of this checksum as computer by the algorithm
	 * specified in {@link Checksum#getAlgorithm()}.
	 * 
	 * @return
	 */
	public String getDigest() {
		return digest;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getAlgorithm(), getDigest());
	}

	@Override
	public boolean equals(Object other) {
		return other != null && other instanceof Checksum && Objects.equals(getAlgorithm(), ((Checksum) other).getAlgorithm())
				&& Objects.equals(getDigest(), ((Checksum) other).getDigest());
	}

}

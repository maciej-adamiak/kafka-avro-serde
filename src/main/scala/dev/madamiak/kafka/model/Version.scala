package dev.madamiak.kafka.model

/**
  * Semantic versioning specification partial implementation 
  *
  * @param major version when you make incompatible API changes
  * @param minor version when you add functionality in a backwards-compatible manner, and
  * @param patch version when you make backwards-compatible bug fixes.
  * @param pre   additional labels for pre-release
  * @see https://semver.org/
  */
class Version(val major: Int, val minor: Int, val patch: Int, val pre: Option[String] = None) extends Ordered[Version] {

  require(major >= 0)
  require(minor >= 0)
  require(patch >= 0)

  override def toString: String = {
    val preRelease = pre.filter(_.nonEmpty).fold("")(x => s"-$x")
    s"$major.$minor.$patch$preRelease"
  }

  override def equals(other: Any): Boolean = other match {
    case that: Version =>
      (that canEqual this) &&
        major == that.major &&
        minor == that.minor &&
        patch == that.patch &&
        pre == that.pre
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[Version]

  override def hashCode(): Int = {
    val state = Seq(major, minor, patch, pre)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  override def compare(that: Version): Int = {
    if (this.major != that.major) this.major.compareTo(that.major)
    else if (this.minor != that.minor) this.minor.compareTo(that.minor)
    else if (this.patch != that.patch) this.patch.compareTo(that.patch)
    else if (this.pre != that.pre) implicitly[Ordering[Option[String]]].compare(this.pre, that.pre)
    else 0
  }
}

object Version {
  private val semanticVersioning = """(\d+)?\.(\d+)?\.(\d+$|\d+)?\-?(\w+)?""".r

  def apply(version: String): Version = version match {
    case semanticVersioning(major, minor, patch, pre) => Version(major.toInt, minor.toInt, patch.toInt, pre)
    case _ => throw new IllegalArgumentException(s"cannot create version object from string $version")
  }

  def apply(major: Int, minor: Int, patch: Int, pre: String = null): Version = new Version(major, minor, patch, Option.apply(pre))
}


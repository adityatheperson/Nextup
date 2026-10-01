# NextUp website

Static product site for the NextUp Android app. No build step, framework, external font, analytics, or backend is required.

## Preview

From the repository root, serve `website/` using a local static HTTP server, for example:

```sh
python -m http.server 8000 --directory website
```

Open http://localhost:8000. ES modules require HTTP rather than opening index.html as a local file.

Demo policy tests run from the repository root:

```sh
node --test scripts/website-demo.test.mjs
```

## Publish

GitHub Pages serves the root of the `gh-pages` branch. That branch contains only the `website/` subtree. After committing website changes on main:

```sh
git subtree push --prefix website origin gh-pages
```

Repository Settings → Pages uses “Deploy from a branch”, branch `gh-pages`, folder `/ (root)`. The public site is https://adityatheperson.github.io/Nextup/.

All asset URLs are relative, including the two JavaScript modules. `.nojekyll` disables Jekyll processing. Repository source, local APKs, private screenshots, and build caches are not deployed.

## Content maintenance

The demo is illustrative, uses fictional class names, and follows Upcoming / Now / Finished states. Keep its walking-selection rule aligned with the Android notification: only the next unstarted event has a walk.

There was no public APK release when this site was prepared. Until a release asset is verified, retain the source/build links rather than adding a nonfunctional download button. Check the displayed app version when delivering future app releases.

package com.example.game.diorama

internal object CourtyardShaders {
  val shadowVertex = """#version 300 es
    layout(location=0) in vec3 point;
    uniform mat4 sunMatrix;
    out vec2 casterXZ;
    void main() {
      casterXZ = point.xz;
      gl_Position = sunMatrix * vec4(point, 1.0);
    }
  """.trimIndent()

  val shadowFragment = """#version 300 es
    precision highp float;
    uniform int dappled;
    uniform vec4 marketBounds;
    in vec2 casterXZ;

    float hash(vec2 cell) {
      return fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
    }

    float clusters(vec2 p) {
      vec2 cell = floor(p);
      vec2 k = smoothstep(0.0, 1.0, fract(p));
      return mix(mix(hash(cell), hash(cell + vec2(1, 0)), k.x),
        mix(hash(cell + vec2(0, 1)), hash(cell + vec2(1, 1)), k.x), k.y);
    }

    void main() {
      if (dappled == 0 || any(lessThan(casterXZ, marketBounds.xy)) ||
        any(greaterThan(casterXZ, marketBounds.zw))) return;
      // The shadow is a perforated proxy of the actual canopy. All its layers
      // share light-space holes, baked once so they cannot swim with the camera.
      vec2 p = gl_FragCoord.xy / vec2(11.0, 15.0);
      float holes = clusters(p) * .77 + clusters(p * 2.37 + vec2(4.6, 7.2)) * .23;
      if (holes > .61) discard;
    }
  """.trimIndent()

  val worldVertex = """#version 300 es
    layout(location=0) in vec3 point;
    layout(location=1) in vec3 normal;
    layout(location=2) in vec2 uv;
    layout(location=3) in vec3 color;
    uniform mat4 viewProjection;
    uniform mat4 sunMatrix;
    out vec3 position;
    out vec3 surfaceNormal;
    out vec2 texCoord;
    out vec3 tint;
    out vec4 sunPosition;
    void main() {
      position = point;
      surfaceNormal = normal;
      texCoord = uv;
      tint = color;
      sunPosition = sunMatrix * vec4(point, 1.0);
      gl_Position = viewProjection * vec4(point, 1.0);
    }
  """.trimIndent()

  // The RGBA8 path compands linear radiance so values over 1 survive on ES 3.0
  // devices where RGBA16F is not color-renderable. All math uses decoded light.
  private val radiance = """
    uniform int packedRadiance;
    vec3 storeLight(vec3 light) {
      return packedRadiance == 0 ? light : light / (1.0 + light);
    }
    vec3 readLight(sampler2D image, vec2 p) {
      vec3 value = texture(image, p).rgb;
      return packedRadiance == 0 ? value : value / max(1.0 - value, vec3(.004));
    }
    float luminance(vec3 light) { return dot(light, vec3(.2126, .7152, .0722)); }
  """.trimIndent()

  val worldFragment = """#version 300 es
    precision highp float;
    precision highp sampler2D;
    $radiance
    uniform sampler2D material;
    uniform sampler2D sunDepth;
    uniform sampler2D heroAtlas;
    uniform vec4 heroFrame;
    uniform vec3 heroSide;
    uniform vec2 heroHeight;
    uniform vec3 camera;
    uniform vec3 actor;
    uniform vec3 sunDirection;
    uniform int kind;
    uniform float uvScale;
    uniform float timer;
    uniform vec3 lamps[4];
    uniform sampler2D marketContact;
    uniform vec4 marketBounds;
    uniform float marketView;
    uniform vec3 fountain;
    uniform float surfaceSheen;
    in vec3 position;
    in vec3 surfaceNormal;
    in vec2 texCoord;
    in vec3 tint;
    in vec4 sunPosition;
    out vec4 pixel;

    float plazaLight(vec2 p) {
      vec2 inside = min(p - marketBounds.xy, marketBounds.zw - p);
      return smoothstep(-.75, 1.05, min(inside.x, inside.y));
    }

    float visibility(float cosine, float plaza) {
      vec3 uvz = sunPosition.xyz / sunPosition.w * 0.5 + 0.5;
      if (any(lessThan(uvz, vec3(0.0))) || any(greaterThan(uvz, vec3(1.0)))) return 1.0;
      float bias = mix(.00192, .00067, cosine);
      vec2 texel = 1.0 / vec2(textureSize(sunDepth, 0));
      float reference = uvz.z - bias;
      float blocker = texture(sunDepth, uvz.xy).r;
      float separation = max(0.0, reference - blocker) * 87.0;
      vec2 spread = texel * mix(1.35, 1.05 + min(2.30, .77 * separation), plaza);
      float visible = 2.0 * step(reference, blocker);
      visible += step(reference, texture(sunDepth, uvz.xy + spread).r);
      visible += step(reference, texture(sunDepth, uvz.xy - spread).r);
      visible += step(reference, texture(sunDepth, uvz.xy + vec2(-spread.x, spread.y)).r);
      visible += step(reference, texture(sunDepth, uvz.xy + vec2(spread.x, -spread.y)).r);
      return visible / 6.0;
    }

    float actorShadow(vec2 delta) {
      if (dot(delta, delta) > 13.0) return 0.0;
      vec2 side = heroSide.xy;
      vec2 forward = vec2(-side.y, side.x);
      float approach = dot(sunDirection.xz, forward);
      if (abs(approach) < .08) return 0.0;
      float ray = -dot(delta, forward) / approach;
      if (ray <= 0.0) return 0.0;
      vec2 k = vec2(dot(delta + sunDirection.xz * ray, side) / (2.0 * heroSide.z) + .5,
        (position.y + ray * sunDirection.y - heroHeight.x) / (heroHeight.y - heroHeight.x));
      if (any(lessThan(k, vec2(0.0))) || any(greaterThan(k, vec2(1.0)))) return 0.0;
      vec2 uv = mix(heroFrame.xy, heroFrame.zw, k);
      vec2 blur = vec2(.00135, .0) * mix(.68, 1.30, k.y);
      float mask = texture(heroAtlas, uv).a * .50
        + texture(heroAtlas, uv - blur).a * .25 + texture(heroAtlas, uv + blur).a * .25;
      return smoothstep(.08, .91, mask);
    }

    vec3 lamplight(vec3 n) {
      float spill = 0.0;
      for (int i = 0; i < 4; ++i) {
        vec3 ray = lamps[i] - position;
        float square = dot(ray, ray);
        if (square >= 7.2) continue;
        float falloff = 1.0 - smoothstep(0.08, 7.2, square);
        spill += falloff * falloff * max(0.0, dot(n, ray) * inversesqrt(max(square, .04)));
      }
      return vec3(1.1, .44, .12) * spill;
    }

    float canopyReveal() {
      vec3 center = actor + vec3(0.0, .78, 0.0);
      vec3 ray = normalize(camera - center);
      vec3 offset = position - center;
      float along = dot(ray, offset);
      if (along <= .18 || along >= 8.5) return 0.0;
      vec3 side = offset - along * ray;
      return .96 * (1.0 - smoothstep(.48, .96, length(side)));
    }

    float ringWave(vec2 point, vec2 strike, float time) {
      float radius = length(point - strike);
      float phase = radius * 19.0 - time * 2.5;
      float filterWeight = 1.0 - smoothstep(.32, 1.42, fwidth(phase));
      return (1.0 - smoothstep(.08, .61, radius)) * sin(phase) * filterWeight;
    }

    vec3 fountainSurface(vec2 point, float shadow) {
      float radius = length(point);
      float phaseX = dot(point, vec2(5.4, 2.9)) + timer * .72;
      float phaseZ = dot(point, vec2(-1.9, 6.0)) - timer * .53;
      float rim = smoothstep(.91, fountain.z * .84, radius);
      vec3 color = mix(vec3(.036, .174, .211), vec3(.077, .269, .244), rim);
      float rings = ringWave(point, vec2(.535, .927), timer)
        + ringWave(point, vec2(.535, -.927), timer + .64)
        + ringWave(point, vec2(-1.07, 0), timer + 1.25);
      vec3 n = normalize(vec3(.068 * cos(phaseX) + .022 * rings,
        1.0, .080 * cos(phaseZ) - .026 * rings));
      vec3 eye = normalize(camera - position);
      float facing = max(dot(n, eye), 0.0);
      float fresnel = pow(1.0 - facing, 3.0);
      float mainGlint = pow(max(dot(n, normalize(sunDirection + eye)), 0.0), 19.0);
      float litRim = smoothstep(.25, 1.30, dot(point, normalize(sunDirection.xz)));
      // A broad sky reflection plus a small warm highlight near the lit rim.
      float sheen = .11 * fresnel + shadow * (.023 + .087 * litRim) *
        (1.0 + .42 * sin(phaseX) * cos(phaseZ));
      color *= .68 + .32 * shadow;
      color += vec3(.26, .64, .72) * (sheen + .017 * rings);
      color += vec3(1.00, .90, .67) * shadow * (mainGlint * .78 + .075 * litRim);
      return max(color, vec3(0.0));
    }

    void main() {
      vec2 surfaceUv = kind == 1 ? position.xz * .62 : texCoord;
      vec4 source = texture(material, surfaceUv * uvScale);
      if (source.a < .45) discard;
      if (kind == 5) {
        // Keep alpha cutout in the same depth buffer while the actor passes behind foliage.
        float screenDoor = fract(dot(floor(gl_FragCoord.xy), vec2(.75487766, .56984030)));
        if (screenDoor < canopyReveal()) discard;
      }
      vec3 base = pow(max(source.rgb * tint, vec3(.001)), vec3(2.2));
      vec3 n = normalize(surfaceNormal);
      float lambert = max(dot(n, sunDirection), 0.0);
      float plaza = plazaLight(position.xz);
      float shadow = visibility(lambert, plaza);
      vec3 ambient = mix(vec3(.22, .21, .22), vec3(.32, .43, .57), .5 + .5 * n.y);
      vec3 skylight = mix(vec3(.18, .225, .285), vec3(.31, .385, .485), .5 + .5 * n.y);
      ambient = mix(ambient, skylight, plaza);
      vec3 sunlight = mix(vec3(4.04, 3.12, 2.15), vec3(2.65, 2.31, 1.95), plaza) * lambert * shadow;
      if (kind == 1) {
        vec2 dx = (position.xz - actor.xz) / vec2(.38, .28);
        float contact = exp(-dot(dx, dx) * 1.7) * (1.0 - smoothstep(.035, .26, abs(position.y - actor.y)));
        ambient *= 1.0 - .50 * contact;
        if (plaza > .001) {
          vec2 patchUv = (position.xz - marketBounds.xy) / (marketBounds.zw - marketBounds.xy);
          float staticContact = texture(marketContact, patchUv).r * plaza;
          ambient *= 1.0 - .40 * staticContact;
          sunlight *= 1.0 - .18 * staticContact;
        }
        float silhouette = actorShadow(position.xz - actor.xz);
        sunlight *= 1.0 - max(.88 * contact, .93 * silhouette);
      } else if (kind == 2) {
        float t = timer * .46;
        float a = position.z * 5.1 + sin(position.x * 1.64) + t;
        float b = position.x * 3.8 - position.z * 2.7 + t * .71;
        n = normalize(vec3(.15 * cos(b), 1.0, .24 * cos(a)));
        vec3 viewDirection = normalize(camera - position);
        float fresnel = pow(1.0 - max(0.0, dot(n, viewDirection)), 3.0);
        float glint = pow(max(dot(n, normalize(sunDirection + viewDirection)), 0.0), 21.0);
        base = mix(base, vec3(.10, .25, .28), fresnel * .43);
        sunlight = vec3(.82, .87, .82) + vec3(1.32, 1.09, .66) * glint * shadow;
      } else if (kind == 3) {
        ambient = mix(vec3(.95, .96, 1.04), vec3(.92, 1.02, 1.15), plaza);
        sunlight = mix(vec3(1.48, 1.13, .73), vec3(1.16, .99, .79), plaza) * shadow;
      }
      vec3 lit = base * (ambient + sunlight + lamplight(n));
      if (kind == 4) lit = base * mix(4.8, 2.75, plaza);
      if (surfaceSheen > 0.0 && plaza > .01) {
        vec3 halfAngle = normalize(sunDirection + normalize(camera - position));
        float highlight = pow(max(dot(n, halfAngle), 0.0), 9.0);
        lit += vec3(1.0, .92, .79) * surfaceSheen * highlight * shadow * plaza;
      }
      if (kind == 2 && abs(position.y - .31) < .015) {
        vec2 offset = position.xz - fountain.xy;
        if (dot(offset, offset) < fountain.z * fountain.z) lit = fountainSurface(offset, shadow);
      }
      float distanceToEye = distance(camera, position);
      float haze = smoothstep(mix(16.0, 23.0, marketView), mix(48.0, 53.0, marketView), distanceToEye)
        * mix(.46, .267, marketView);
      float coolGroundFog = (1.0 - smoothstep(-.8, 2.0, position.y)) * smoothstep(19.0, 44.0, distanceToEye);
      vec3 air = mix(vec3(.19, .30, .38), vec3(.41, .40, .31), shadow * .46);
      air = mix(air, vec3(.16, .23, .30), marketView * .74);
      pixel = vec4(storeLight(mix(lit, air, haze + (1.0 - haze) * coolGroundFog * mix(.32, .065, marketView))), 1.0);
    }
  """.trimIndent()

  val lensVertex = """#version 300 es
    out vec2 uv;
    void main() {
      vec2 corner = vec2(gl_VertexID == 1 ? 3.0 : -1.0, gl_VertexID == 2 ? 3.0 : -1.0);
      uv = corner * .5 + .5;
      gl_Position = vec4(corner, 0.0, 1.0);
    }
  """.trimIndent()

  val bloomFragment = """#version 300 es
    precision highp float;
    precision highp sampler2D;
    $radiance
    uniform sampler2D rendered;
    uniform vec2 sampleStep;
    uniform int extract;
    in vec2 uv;
    out vec4 pixel;

    vec3 highlights(vec2 p) {
      vec3 color = readLight(rendered, p);
      float value = luminance(color);
      return color * smoothstep(.83, 2.12, value);
    }

    void main() {
      vec3 light;
      if (extract == 1) {
        vec2 d = sampleStep * 1.15;
        light = (highlights(uv + d) + highlights(uv - d) +
          highlights(uv + vec2(d.x, -d.y)) + highlights(uv + vec2(-d.x, d.y))) * .25;
      } else {
        light = readLight(rendered, uv) * .227027;
        light += (readLight(rendered, uv + sampleStep * 1.384615) +
          readLight(rendered, uv - sampleStep * 1.384615)) * .316216;
        light += (readLight(rendered, uv + sampleStep * 3.230769) +
          readLight(rendered, uv - sampleStep * 3.230769)) * .070270;
      }
      pixel = vec4(storeLight(light), 1.0);
    }
  """.trimIndent()

  val lensFragment = """#version 300 es
    precision highp float;
    precision highp sampler2D;
    $radiance
    uniform sampler2D rendered;
    uniform sampler2D depth;
    uniform sampler2D glow;
    uniform sampler2D fogSunDepth;
    uniform mat4 inverseViewProjection;
    uniform mat4 sunMatrix;
    uniform vec3 camera;
    uniform vec2 pixelStep;
    uniform vec2 planes;
    uniform float focus;
    uniform float fountainFocus;
    uniform float marketView;
    uniform float shaftStrength;
    uniform vec3 sunDirection;
    in vec2 uv;
    out vec4 pixel;

    float distanceAt(vec2 p) {
      float d = texture(depth, p).r;
      return (planes.x * planes.y) / mix(planes.y, planes.x, d);
    }

    vec3 colorAt(vec2 p) {
      if (texture(depth, p).r < .99999) return readLight(rendered, p);
      vec3 sky = mix(vec3(.52, .49, .38), vec3(.21, .32, .42), smoothstep(.22, 1.0, p.y));
      vec2 ray = (p - vec2(.07, 1.03)) * vec2(2.2, 3.4);
      return sky + vec3(.27, .20, .10) * exp(-dot(ray, ray)) * (1.0 - marketView * .84);
    }

    vec3 marketShaft(vec3 ray, float rayLength) {
      // One small column of dust just beside the market tree, along the light ray.
      vec3 origin = vec3(-8.70, 1.50, 18.25);
      float nearest = clamp(dot(origin - camera, ray), 0.0, rayLength);
      if (length(camera + ray * nearest - origin) > 4.45) return vec3(0.0);
      float entry = max(0.0, nearest - 3.0);
      float exit = min(rayLength, nearest + 3.0);
      float stepSize = max(0.0, (exit - entry) * .25);
      float scatter = 0.0;
      for (int i = 0; i < 4; ++i) {
        vec3 p = camera + ray * (entry + stepSize * (float(i) + .5));
        vec3 offset = p - origin;
        vec3 axis = offset - sunDirection * dot(offset, sunDirection);
        float width = exp(-dot(axis * vec3(1.6, 1.0, 2.5), axis));
        float height = smoothstep(.36, 1.2, p.y) * (1.0 - smoothstep(3.4, 5.6, p.y));
        vec4 fromSun = sunMatrix * vec4(p, 1.0);
        vec3 lightUv = fromSun.xyz / fromSun.w * .5 + .5;
        float lit = smoothstep(-.0004, .0005, texture(fogSunDepth, lightUv.xy).r - lightUv.z + .0012);
        scatter += width * height * lit;
      }
      return vec3(.85, .71, .48) * scatter * stepSize * .021;
    }

    vec3 sunHaze() {
      vec2 ndc = uv * 2.0 - 1.0;
      vec4 clip = inverseViewProjection * vec4(ndc, texture(depth, uv).r * 2.0 - 1.0, 1.0);
      vec3 segment = clip.xyz / clip.w - camera;
      float rayLength = length(segment);
      vec3 ray = segment / max(rayLength, .001);
      vec3 local = vec3(0.0);
      if (marketView > .001 && shaftStrength > .001) {
        local = marketShaft(ray, rayLength) * marketView * shaftStrength;
      }
      if (marketView > .999) return local;
      float end = min(rayLength, 38.0);
      float scatter = 0.0;
      for (int i = 0; i < 4; ++i) {
        vec3 p = camera + ray * end * (float(i) + .5) * .25;
        // Limit scattering to the stream and the gap between roofs.
        vec2 axis = (p.xz + p.y * vec2(1.5, -.94) - vec2(-2.0, -3.1)) * vec2(.42, .17);
        float density = exp(-dot(axis, axis)) * (1.0 - smoothstep(1.6, 8.0, p.y));
        vec4 light = sunMatrix * vec4(p, 1.0);
        vec3 lightUv = light.xyz / light.w * .5 + .5;
        float lit = step(lightUv.z - .0012, texture(fogSunDepth, lightUv.xy).r);
        scatter += density * lit;
      }
      return local + vec3(.76, .52, .26) * scatter * end * .0019 * (1.0 - marketView);
    }

    void main() {
      float center = distanceAt(uv);
      float defocus = center < focus ? focus - center : (center - focus) * .81;
      float playBand = max(0.0, abs(center - fountainFocus) - 2.25);
      defocus = mix(defocus, min(defocus, playBand), marketView);
      float radius = smoothstep(mix(2.25, 3.20, marketView), mix(10.0, 15.5, marketView), defocus)
        * mix(6.0, 2.8, marketView);
      vec3 color = colorAt(uv);
      if (radius > .35) {
        vec3 sum = color * 2.0;
        float weight = 2.0;
        for (int i = 0; i < 8; ++i) {
          float angle = float(i) * .785398;
          vec2 q = uv + vec2(cos(angle), sin(angle)) * pixelStep * radius;
          float other = distanceAt(q);
          // Weight similar depths so blurred roofs and railings do not leak onto the player.
          float w = 1.0 - smoothstep(.85, 3.0, abs(other - center));
          sum += colorAt(q) * w;
          weight += w;
        }
        color = sum / weight;
      }
      color += readLight(glow, uv) * mix(.22, .145, marketView) + sunHaze();
      float edge = smoothstep(.31, .74, length((uv - vec2(.48, .48)) * vec2(1.0, .89)));
      color *= 1.0 - .24 * edge;
      // Tone mapping compresses highlights before conversion from linear light to display color.
      vec3 exposed = color * 1.04;
      color = (exposed * (2.51 * exposed + .03)) / (exposed * (2.43 * exposed + .59) + .14);
      pixel = vec4(pow(clamp(color, 0.0, 1.0), vec3(1.0 / 2.2)), 1.0);
    }
  """.trimIndent()
}

package com.example.game.diorama

internal object CourtyardShaders {
  val shadowVertex = """#version 300 es
    layout(location=0) in vec3 point;
    uniform mat4 sunMatrix;
    void main() { gl_Position = sunMatrix * vec4(point, 1.0); }
  """.trimIndent()

  val shadowFragment = """#version 300 es
    precision mediump float;
    void main() {}
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

  val worldFragment = """#version 300 es
    precision highp float;
    uniform sampler2D material;
    uniform sampler2D sunDepth;
    uniform vec3 camera;
    uniform vec3 actor;
    uniform vec3 sunDirection;
    uniform int kind;
    uniform float timer;
    in vec3 position;
    in vec3 surfaceNormal;
    in vec2 texCoord;
    in vec3 tint;
    in vec4 sunPosition;
    out vec4 pixel;

    float visibility(float cosine) {
      vec3 uvz = sunPosition.xyz / sunPosition.w * 0.5 + 0.5;
      if (any(lessThan(uvz, vec3(0.0))) || any(greaterThan(uvz, vec3(1.0)))) return 1.0;
      float bias = mix(.0034, .0014, cosine);
      float visible = 0.0;
      for (int i = 0; i < 4; ++i) {
        vec2 offset = vec2(float(i % 2) - .5, float(i / 2) - .5) * .00165;
        visible += step(uvz.z - bias, texture(sunDepth, uvz.xy + offset).r);
      }
      return visible * .25;
    }

    void main() {
      vec4 source = texture(material, kind == 4 ? position.xz * .71 : texCoord);
      if (kind == 4) {
        float ragged = .08 + fract(sin(dot(floor(position.xz * 28.0), vec2(127.1, 311.7))) * 43758.5453) * .13;
        if (min(texCoord.x, 1.0 - texCoord.x) < ragged) discard;
      }
      if (source.a < .45) discard;
      vec3 base = source.rgb * tint;
      vec3 n = normalize(surfaceNormal);
      float lambert = max(dot(n, sunDirection), 0.0);
      float shadow = visibility(lambert);
      vec3 ambient = vec3(.48, .54, .58) * mix(.78, 1.10, max(0.0, n.y));
      vec3 sunlight = vec3(.78, .68, .52) * lambert * shadow;
      if (kind == 1 || kind == 4) {
        vec2 dx = (position.xz - actor.xz) / vec2(.38, .27);
        float contact = exp(-dot(dx, dx) * 1.7) * (1.0 - smoothstep(.02, .28, abs(position.y - actor.y)));
        ambient *= 1.0 - .37 * contact;
        sunlight *= 1.0 - .78 * contact;
      } else if (kind == 2) {
        float wave = sin(position.z * 4.1 + sin(position.x * 1.3) + timer * .6);
        float glint = pow(max(0.0, wave), 22.0);
        base += vec3(.05, .095, .097) * glint;
      } else if (kind == 3) {
        // Keep the sprite pixels and lighting readable as the billboard rotates with the camera.
        ambient = vec3(.74, .75, .75);
        sunlight = vec3(.34, .30, .23) * shadow;
      }
      vec3 lit = base * (ambient + sunlight);
      float air = smoothstep(16.0, 47.0, distance(camera, position)) * .43;
      pixel = vec4(mix(lit, vec3(.42, .50, .49), air), 1.0);
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

  val lensFragment = """#version 300 es
    precision highp float;
    uniform sampler2D rendered;
    uniform sampler2D depth;
    uniform vec2 pixelStep;
    uniform float focus;
    in vec2 uv;
    out vec4 pixel;

    float distanceAt(vec2 p) {
      float d = texture(depth, p).r;
      return (2.0 * .65 * 80.0) / (80.0 + .65 - (2.0 * d - 1.0) * (80.0 - .65));
    }

    vec3 colorAt(vec2 p) {
      return texture(depth, p).r >= .99999
        ? mix(vec3(.63, .65, .51), vec3(.29, .43, .49), smoothstep(.2, 1.0, p.y))
        : texture(rendered, p).rgb;
    }

    void main() {
      float center = distanceAt(uv);
      float radius = smoothstep(1.5, 10.0, abs(center - focus)) * 4.2;
      vec3 color = colorAt(uv);
      if (radius > .25) {
        vec3 sum = color * 2.0;
        float weight = 2.0;
        for (int i = 0; i < 8; ++i) {
          float angle = float(i) * .785398;
          vec2 q = uv + vec2(cos(angle), sin(angle)) * pixelStep * radius;
          float delta = abs(distanceAt(q) - center);
          // A far blurred wall may not smear into the playable foreground.
          float w = mix(1.0, .055, smoothstep(.65, 3.2, delta));
          sum += colorAt(q) * w;
          weight += w;
        }
        color = sum / weight;
      }
      float edges = smoothstep(.29, .73, length((uv - vec2(.47, .49)) * vec2(1.0, .86)));
      vec2 beam = uv - vec2(.18, .78);
      float haze = exp(-dot(beam * vec2(2.5, 6.6), beam * vec2(2.5, 6.6))) * .12;
      color = mix(color * (1.0 - .35 * edges), vec3(.93, .80, .53), haze);
      pixel = vec4(color, 1.0);
    }
  """.trimIndent()
}
